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

package akka.persistence.query

import akka.persistence.query.scaladsl.{CurrentEventsByTagQuery, EventsByTagQuery}
import akka.stream.testkit.scaladsl.TestSink
import scala.concurrent.duration._

/**
 * Test case for [[CurrentEventsByTagQuery]] and [[EventsByTagQuery]].
 */
trait EventsByTagSpec { _: ReadJournalSpec =>

  "CurrentEventsByTagQuery" should "fetch all current events" in {
    val pId = newPersistenceId

    persist(pId, 2, Set(pId))
    persist(pId, 1, Set(s"$pId-non"))
    persist(pId, 1, Set(pId))

    readJournal.currentEventsByTag(pId, NoOffset)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .request(3)
        .expectNextN(expectedEvents(pId, 1, 2))
        .expectNextN(expectedEvents(pId, 4, 4))
        .expectComplete()
  }

  it should "fetch events starting at the given offset" in {
    val pId = newPersistenceId

    persist(pId, 5, Set(pId))

    val offset = readJournal.currentEventsByTag(pId, NoOffset)
        .map(envelope => envelope.offset.asInstanceOf[Sequence].value)
        .runWith(TestSink.probe[Long])
        .request(5)
        .expectNextN(5)(2)

    readJournal.currentEventsByTag(pId, Sequence(offset))
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .request(4)
        .expectNextN(expectedEvents(pId, 3, 5))
        .expectComplete()
  }

  it should "not fetch events that were added after" in {
    val pId = newPersistenceId

    persist(pId, 4, Set(pId))

    readJournal.currentEventsByTag(pId, NoOffset)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .request(5)
        .expectNextN(expectedEvents(pId, 1, 4))
        .expectComplete()

    persist(pId, 1, Set(pId))
  }

  it should "not see any events if the stream starts after the highest offset" in {
    val pId = newPersistenceId

    persist(pId, 4, Set(pId))

    val offset = readJournal.currentEventsByTag(pId, NoOffset)
        .map(envelope => envelope.offset.asInstanceOf[Sequence].value)
        .runWith(TestSink.probe[Long])
        .request(4)
        .expectNextN(4).last

    readJournal.currentEventsByTag(pId, Sequence(offset + 1))
        .runWith(TestSink.probe)
        .expectSubscriptionAndComplete()
  }

  it should "only fetch what is requested even if there is more in the buffer" in {
    val pId = newPersistenceId

    persist(pId, 4, Set(pId))

    val probe = readJournal.currentEventsByTag(pId, NoOffset)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])

    probe
        .request(2)
        .expectNextN(expectedEvents(pId, 1, 2))
        .expectNoMessage(300.millis)

    probe
        .request(2)
        .expectNextN(expectedEvents(pId, 3, 4))
        .expectComplete()
  }

  it should "complete if no events are found" in {
    val pId = newPersistenceId

    readJournal.currentEventsByTag(pId, NoOffset)
        .runWith(TestSink.probe)
        .expectSubscriptionAndComplete()
  }

  "EventsByTagQuery" should "keep running even if the tag does not exist yet" in {
    val pId = newPersistenceId

    readJournal.eventsByTag(pId, NoOffset)
        .runWith(TestSink.probe)
        .request(1)
        .expectNoMessage(300.millis)
        .cancel()
  }

  it should "fetch all existing events and keep fetching new ones" in {
    val pId = newPersistenceId

    persist(pId, 4, Set(pId))

    val probe = readJournal.eventsByTag(pId, NoOffset)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])

    probe.request(5)
        .expectNextN(expectedEvents(pId, 1, 4))
        .expectNoMessage(300.millis)

    persist(pId, 1, Set(pId))

    probe
        .expectNextN(expectedEvents(pId, 5, 5))
        .cancel()
  }

  it should "fetch events starting at the given offset" in {
    val pId = newPersistenceId

    persist(pId, 4, Set(pId))

    val offset = readJournal.currentEventsByTag(pId, NoOffset)
        .map(envelope => envelope.offset.asInstanceOf[Sequence].value)
        .runWith(TestSink.probe[Long])
        .request(2)
        .expectNextN(2).last

    val probe = readJournal.eventsByTag(pId, Sequence(offset))
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])

    probe.request(4)
        .expectNextN(expectedEvents(pId, 2, 4))
        .expectNoMessage(300.millis)

    persist(pId, 1, Set(pId))

    probe
        .expectNextN(expectedEvents(pId, 5, 5))
        .cancel()
  }

  it should "only fetch what is requested even if there is more in the buffer" in {
    val pId = newPersistenceId

    persist(pId, 4, Set(pId))

    val probe = readJournal.eventsByTag(pId, NoOffset)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])

    probe.request(5)
        .expectNextN(expectedEvents(pId, 1, 4))
        .expectNoMessage(300.millis)

    persist(pId, 4, Set(pId))

    probe
        .expectNextN(expectedEvents(pId, 5, 5))
        .expectNoMessage(300.millis)
        .request(3)
        .expectNextN(expectedEvents(pId, 6, 8))
        .expectNoMessage(300.millis)
        .cancel()
  }

  private def expectedEvents(pId: String, fromSeq: Long, toSeq: Long): Seq[(Long, Any)] =
    for (i <- fromSeq to toSeq) yield (i, s"$pId-$i")

}
