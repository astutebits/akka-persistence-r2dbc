package akka.persistence.r2dbc.query

import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import scala.concurrent.duration.FiniteDuration

private[akka] object EventsByTagStage {

  private val PollTimerKey = "FetchEvents"

  def apply(
      dao: QueryDao,
      tag: String,
      offset: Long,
      refreshInterval: Option[FiniteDuration]
  ): EventsByTagStage = new EventsByTagStage(dao, tag, offset, refreshInterval)

}

/**
 * Walks the journal entries returning any events that match the given tag.
 */
private[akka] final class EventsByTagStage private(
    dao: QueryDao,
    tag: String,
    offset: Long,
    refreshInterval: Option[FiniteDuration] = None
) extends GraphStage[SourceShape[JournalEntry]] {
  require(tag != null && tag.nonEmpty, "the 'tag' must be provided")
  require(offset >= 0, "the 'offset' must be >= 0")

  import EventsByTagStage.PollTimerKey

  private val out: Outlet[JournalEntry] = Outlet("Event.out")

  override def createLogic(attributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) with InHandler with OutHandler {
    private var sinkIn: SubSinkInlet[JournalEntry] = _
    private var currentCycle = 0
    private var currentIndex = offset
    private var targetIndex: Long = 0

    // Initial handler (until the SinkInlet is attached)
    setHandler(out, new OutHandler {
      def onPull(): Unit = {
      }
    })

    override def preStart(): Unit = {
      fetchEvents()
    }

    override def postStop(): Unit = {
      if (!sinkIn.isClosed)
        sinkIn.cancel()
    }

    override protected def onTimer(timerKey: Any): Unit = timerKey match {
      case PollTimerKey =>
        fetchEvents()
    }

    override def onPush(): Unit = {
      val entry = sinkIn.grab()
      currentIndex = entry.index
      push(out, entry)
    }

    override def onPull(): Unit = {
      if (!sinkIn.isClosed)
        sinkIn.pull()
    }

    override def onUpstreamFinish(): Unit = {
      if (currentIndex == targetIndex && refreshInterval.isEmpty) {
        completeStage()
      } else {
        refreshInterval.foreach(interval => {
          currentCycle += 1
          scheduleOnce(PollTimerKey, interval)
        })
      }
    }

    override def onUpstreamFailure(ex: Throwable): Unit = {
      cancelTimer(PollTimerKey)
      failStage(ex)
    }

    private def fetchEvents(): Unit = {
      sinkIn = new SubSinkInlet[JournalEntry](s"Event.in")

      Source.future(dao.findHighestIndex(tag).runWith(Sink.last)(subFusingMaterializer))
          .flatMapConcat(result => {
            if (targetIndex == result) {
              Source.empty
            } else {
              targetIndex = result
              dao.fetchByTag(tag, currentIndex, result)
            }
          })
          .to(sinkIn.sink)
          .run()(subFusingMaterializer)

      if (isAvailable(out))
        sinkIn.pull()

      setHandler(out, this)
      sinkIn.setHandler(this)
    }
  }

  override def shape: SourceShape[JournalEntry] = SourceShape(out)

}
