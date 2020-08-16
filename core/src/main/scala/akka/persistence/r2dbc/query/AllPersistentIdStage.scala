package akka.persistence.r2dbc.query

import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

private[akka] object AllPersistentIdStage {
  def apply(
      dao: QueryDao,
      refreshInterval: Option[FiniteDuration] = None
  ): AllPersistentIdStage = new AllPersistentIdStage(dao, refreshInterval)

  private case object PollTimerKey
}

private[query] final class AllPersistentIdStage private(
    dao: QueryDao,
    refreshInterval: Option[FiniteDuration] = None
) extends GraphStage[SourceShape[String]] {

  require(dao != null, "the 'dao' must be provided")

  import AllPersistentIdStage.PollTimerKey

  private val out: Outlet[String] = Outlet("PersistenceId.out")

  private val knownIds: mutable.Set[String] = mutable.HashSet.empty[String]
  private var offset: Long = -1

  override def createLogic(attributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) with InHandler with OutHandler {
    private var sinkIn: SubSinkInlet[String] = _

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
      push(out, sinkIn.grab())
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
          scheduleOnce(PollTimerKey, interval)
        })
      }
    }

    private def runStage(): Unit = {
      sinkIn = new SubSinkInlet[String]("PersistenceId.in")

      dao.fetchPersistenceIds(offset + 1)
          .filterNot(x => knownIds(x._2))
          .map(x => {
            offset = x._1
            knownIds += x._2
            x._2
          })
          .to(sinkIn.sink).run()(subFusingMaterializer)

      if (isAvailable(out))
        sinkIn.pull()

      setHandler(out, this)
      sinkIn.setHandler(this)
    }
  }

  override def shape: SourceShape[String] = SourceShape(out)

}
