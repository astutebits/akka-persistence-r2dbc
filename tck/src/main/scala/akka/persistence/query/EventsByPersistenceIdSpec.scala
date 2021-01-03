/*
 * Copyright 2020-2021 Borislav Borisov.
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

import akka.persistence.query.scaladsl.{CurrentEventsByPersistenceIdQuery, EventsByPersistenceIdQuery}
import akka.stream.testkit.scaladsl.TestSink
import scala.concurrent.duration._
import scala.collection.immutable

/**
 * Test case for [[CurrentEventsByPersistenceIdQuery]] and [[EventsByPersistenceIdQuery]].
 */
trait EventsByPersistenceIdSpec { _: ReadJournalSpec =>

  "CurrentEventsByPersistenceIdQuery" should "fetch existing subset of events" in {
    val pId = newPersistenceId

    persist(pId, 5)

    readJournal.currentEventsByPersistenceId(pId, 2, 4)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNextN(expectedEvents(pId, 2, 4))
        .expectComplete()
  }

  it should "fetch events from a sequence number" in {
    val pId = newPersistenceId

    persist(pId, 5)

    readJournal.currentEventsByPersistenceId(pId, 3, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNextN(expectedEvents(pId, 3, 5))
        .expectComplete()
  }

  it should "fetch events up to a sequence number" in {
    val pId = newPersistenceId

    persist(pId, 3)

    readJournal.currentEventsByPersistenceId(pId, 0, 2)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNextN(expectedEvents(pId, 1, 2))
        .expectComplete()
  }

  it should "fetch all events" in {
    val pId = newPersistenceId

    persist(pId, 5)

    readJournal.currentEventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNextN(expectedEvents(pId, 1, 5))
        .expectComplete()
  }

  it should "only fetch what is requested even if there is more in the buffer" in {
    val pId = newPersistenceId

    persist(pId, 5)

    val probe = readJournal.currentEventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])

    probe
        .request(2)
        .expectNextN(expectedEvents(pId, 1, 2))
        .expectNoMessage(300.millis)

    probe
        .request(3)
        .expectNextN(expectedEvents(pId, 3, 5))
        .expectComplete()
  }

  it should "complete if no events are found" in {
    val pId = newPersistenceId

    readJournal.currentEventsByPersistenceId(pId, 0, Long.MaxValue)
        .runWith(TestSink.probe[EventEnvelope])
        .ensureSubscription()
        .request(10)
        .expectComplete()
  }

  it should "not see any events if the stream starts after current latest event" in {
    val pId = newPersistenceId

    persist(pId, 5)

    readJournal.currentEventsByPersistenceId(pId, 6, Long.MaxValue)
        .runWith(TestSink.probe[EventEnvelope])
        .ensureSubscription()
        .request(10)
        .expectComplete()
  }

  "EventsByPersistenceIdQuery" should "fetch events indefinitely" in {
    val pId = newPersistenceId

    persist(pId, 3)

    val probe = readJournal.eventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()

    probe
        .request(5)
        .expectNextN(expectedEvents(pId, 1, 3))
        .expectNoMessage(300.millis)

    persist(pId, 1)

    probe
        .expectNextN(expectedEvents(pId, 4, 4))
        .expectNoMessage(300.millis)

    persist(pId, 1)

    probe
        .expectNextN(expectedEvents(pId, 5, 5))
        .expectNoMessage(300.millis)

    probe.cancel()
  }

  it should "fetch events after the current latest event" in {
    val pId = newPersistenceId

    persist(pId, 3)

    val probe = readJournal.eventsByPersistenceId(pId, 4, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(5)
        .expectNoMessage(300.millis)

    persist(pId, 2)

    probe
        .expectNextN(expectedEvents(pId, 4, 5))
        .expectNoMessage(300.millis)

    probe.cancel()
  }

  it should "fetch a subset of events" in {
    val pId = newPersistenceId

    persist(pId, 3)

    val probe = readJournal.eventsByPersistenceId(pId, 1, 4)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(5)
        .expectNextN(expectedEvents(pId, 1, 3))
        .expectNoMessage(300.millis)

    persist(pId, 1)

    probe
        .expectNextN(expectedEvents(pId, 4, 4))
        .expectComplete()
  }

  it should "fetch events after demand request" in {
    val pId = newPersistenceId

    persist(pId, 3)

    val probe = readJournal.eventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(2)
        .expectNextN(expectedEvents(pId, 1, 2))
        .expectNoMessage(300.millis)

    persist(pId, 1)

    probe
        .request(5)
        .expectNextN(expectedEvents(pId, 3, 4))
        .expectNoMessage(300.millis)

    probe.cancel()
  }

  it should "only deliver what is requested even if there is more in the buffer" in {
    val pId = newPersistenceId

    persist(pId, 10)

    val probe = readJournal.eventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(2)
        .expectNextN(expectedEvents(pId, 1, 2))
        .expectNoMessage(300.millis)

    probe
        .request(5)
        .expectNextN(expectedEvents(pId, 3, 7))
        .expectNoMessage(300.millis)

    probe.request(3)
        .expectNextN(expectedEvents(pId, 8, 10))
        .expectNoMessage(300.millis)

    probe.cancel()
  }

  it should "not fetch anything if there aren't any events" in {
    val pId = newPersistenceId

    val probe = readJournal.eventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNoMessage(300.millis)

    probe.cancel()
  }

  it should "fetch events in the correct order" in {
    val pId = newPersistenceId

    persist(pId, 3)

    val events = readJournal.eventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event, envelope.offset.asInstanceOf[Sequence]))
        .runWith(TestSink.probe[(Long, Any, Sequence)])
        .request(3)
        .expectNextN(3)

    events(1)._3.value should be > events.head._3.value
    events(1)._3.value should be < events.last._3.value
  }

  private def expectedEvents(pId: String, fromSeq: Long, toSeq: Long): immutable.Seq[(Long, Any)] =
    for (i <- fromSeq to toSeq) yield (i, s"$pId-$i")

}
