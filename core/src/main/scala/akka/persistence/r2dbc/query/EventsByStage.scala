/*
 * Copyright 2020 Borislav Borisov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source
import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.FiniteDuration

private[query] object EventsByStage {

  private case object PollTimerKey

}

private[query] abstract class EventsByStage extends GraphStage[SourceShape[JournalEntry]] {

  import EventsByStage.PollTimerKey

  protected val refreshInterval: Option[FiniteDuration]
  protected val completeSwitch: AtomicBoolean

  private val out: Outlet[JournalEntry] = Outlet("Event.out")

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
      if (refreshInterval.isEmpty || completeSwitch.get()) {
        completeStage()
      } else {
        refreshInterval.foreach(interval => {
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

  protected def pushedEntry(entry: JournalEntry): Unit

  protected def fetchEvents(): Source[JournalEntry, NotUsed]

}
