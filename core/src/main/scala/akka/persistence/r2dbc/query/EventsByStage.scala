package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source
import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import scala.concurrent.duration.FiniteDuration

private[akka] object EventsByStage {

  private case object PollTimerKey

}

private[akka] abstract class EventsByStage extends GraphStage[SourceShape[JournalEntry]] {

  import EventsByStage.PollTimerKey

  protected val refreshInterval: Option[FiniteDuration]

  private val out: Outlet[JournalEntry] = Outlet("Event.out")
  private var _currentCycle = 0

  final override def createLogic(attributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) with InHandler with OutHandler {
    private var sinkIn: SubSinkInlet[JournalEntry] = _

    // Initial handler (until the SubSinkInlet is attached)
    setHandler(out, new OutHandler {
      def onPull(): Unit = {
      }
    })

    override def preStart(): Unit = {
      runStage()
    }

    override def postStop(): Unit = {
      if (!sinkIn.isClosed)
        sinkIn.cancel()
    }

    override protected def onTimer(timerKey: Any): Unit = timerKey match {
      case PollTimerKey =>
        runStage()
    }

    override def onPush(): Unit = {
      val entry = sinkIn.grab()
      pushedEntry(entry)
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
          _currentCycle += 1
          scheduleOnce(PollTimerKey, interval)
        })
      }
    }

    override def onUpstreamFailure(ex: Throwable): Unit = {
      cancelTimer(PollTimerKey)
      failStage(ex)
    }

    private def runStage(): Unit = {
      sinkIn = new SubSinkInlet[JournalEntry]("Event.in")

      fetchEvents().to(sinkIn.sink).run()(subFusingMaterializer)

      if (isAvailable(out))
        sinkIn.pull()

      setHandler(out, this)
      sinkIn.setHandler(this)
    }
  }

  final override def shape: SourceShape[JournalEntry] = SourceShape(out)

  final def currentCycle: Long = _currentCycle

  protected def pushedEntry(entry: JournalEntry): Unit

  protected def fetchEvents(): Source[JournalEntry, NotUsed]

}
