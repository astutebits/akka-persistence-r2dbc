package akka.persistence.r2dbc.query

import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import scala.concurrent.duration.FiniteDuration

object EventsByPersistenceIdStage {
  val PollTimerKey = "FetchEvents"

  def apply(
      dao: QueryDao,
      persistenceId: String,
      fromSeqNr: Long,
      toSeqNr: Long,
      refreshInterval: Option[FiniteDuration] = None
  ): EventsByPersistenceIdStage =
    new EventsByPersistenceIdStage(dao, persistenceId, fromSeqNr, toSeqNr, refreshInterval)
}

final class EventsByPersistenceIdStage(
    dao: QueryDao,
    persistenceId: String,
    fromSeqNr: Long,
    toSeqNr: Long,
    refreshInterval: Option[FiniteDuration] = None
) extends GraphStage[SourceShape[JournalEntry]] {
  require(persistenceId != null && persistenceId.nonEmpty, "the 'persistenceId' must be provided")
  require(fromSeqNr >= 0, "the 'fromSeqNr' must be >= 0")
  require(toSeqNr >= 0, "the 'toSeqNr' must be >= 0")
  require(fromSeqNr < toSeqNr, "the 'fromSeqNr' must be < the 'toSeqNr'")

  import EventsByPersistenceIdStage.PollTimerKey

  private val out: Outlet[JournalEntry] = Outlet("Event.out")

  override def createLogic(attributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) with InHandler with OutHandler {
    private var sinkIn: SubSinkInlet[JournalEntry] = _
    private var currentCycle = 0
    private var currentIndex = fromSeqNr
    private var targetIndex: Long = 0

    // Initial handler (until the SubSinkInlet is attached)
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
      currentIndex = entry.sequenceNr
      push(out, entry)
    }

    override def onPull(): Unit = {
      if (!sinkIn.isClosed)
        sinkIn.pull()
    }

    override def onUpstreamFinish(): Unit = {
      if (refreshInterval.isEmpty) {
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

      Source.future(dao.findHighestSeq(persistenceId)
          .runWith(Sink.last)(subFusingMaterializer))
          .flatMapConcat(result => {
            if (targetIndex == result)
              Source.empty
            else {
              targetIndex = if (result > toSeqNr && currentCycle == 0) toSeqNr else result
              dao.fetchByPersistenceId(persistenceId, if (currentCycle == 0) currentIndex else currentIndex + 1, targetIndex)
            }
          })
          .to(sinkIn.sink).run()(subFusingMaterializer)

      if (isAvailable(out))
        sinkIn.pull()

      setHandler(out, this)
      sinkIn.setHandler(this)
    }
  }

  override def shape: SourceShape[JournalEntry] = SourceShape(out)
}
