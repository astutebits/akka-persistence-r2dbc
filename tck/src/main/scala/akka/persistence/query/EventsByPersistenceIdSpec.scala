package akka.persistence.query

import akka.persistence.query.scaladsl.{CurrentEventsByPersistenceIdQuery, EventsByPersistenceIdQuery}
import akka.stream.testkit.scaladsl.TestSink
import java.util.UUID
import scala.concurrent.duration._

/**
 * Test case for [[CurrentEventsByPersistenceIdQuery]] and [[EventsByPersistenceIdQuery]].
 */
trait EventsByPersistenceIdSpec { _: ReadJournalSpec =>

  "Postgresql query currentEventsByPersistenceId" should "implement standard CurrentEventsByPersistenceIdQuery" in {
    readJournal.isInstanceOf[CurrentEventsByPersistenceIdQuery] should ===(true)
  }

  it should "fetch existing subset of events" in {
    val pId = "cebp-a"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, pId, writerUuid)

    readJournal.currentEventsByPersistenceId(pId, 2, 4)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNextN(expectedEvents(pId, 2, 4))
        .expectComplete()
  }

  it should "fetch events from a sequence number" in {
    val pId = "cebp-b"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, pId, writerUuid)

    readJournal.currentEventsByPersistenceId(pId, 3, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNextN(expectedEvents(pId, 3, 5))
        .expectComplete()
  }

  it should "fetch events up to a sequence number" in {
    val pId = "cebp-c"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 3, pId, writerUuid)

    readJournal.currentEventsByPersistenceId(pId, 0, 2)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNextN(expectedEvents(pId, 1, 2))
        .expectComplete()
  }

  it should "fetch all events" in {
    val pId = "cebp-d"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, pId, writerUuid)

    readJournal.currentEventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNextN(expectedEvents(pId, 1, 5))
        .expectComplete()
  }

  it should "only fetch what is requested even if there is more in the buffer" in {
    val pId = "cebp-e"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, pId, writerUuid)

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
    val pId = "cebp-f"

    readJournal.currentEventsByPersistenceId(pId, 0, Long.MaxValue)
        .runWith(TestSink.probe[EventEnvelope])
        .ensureSubscription()
        .request(10)
        .expectComplete()
  }

  it should "not see any events if the stream starts after current latest event" in {
    val pId = "cebp-g"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, pId, writerUuid)

    readJournal.currentEventsByPersistenceId(pId, 6, Long.MaxValue)
        .runWith(TestSink.probe[EventEnvelope])
        .ensureSubscription()
        .request(10)
        .expectComplete()
  }

  "Postgresql query eventsByPersistenceId" should "implement standard EventsByPersistenceIdQuery" in {
    readJournal.isInstanceOf[EventsByPersistenceIdQuery] should ===(true)
  }

  it should "fetch events indefinitely" in {
    val pId = "ebp-a"
    val writerUuid = UUID.randomUUID()
    writeMessages(1, 3, pId, writerUuid)

    val probe = readJournal.eventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()

    probe
        .request(5)
        .expectNextN(expectedEvents(pId, 1, 3))
        .expectNoMessage(300.millis)

    writeMessages(4, 4, pId, writerUuid)

    probe
        .expectNextN(expectedEvents(pId, 4, 4))
        .expectNoMessage(300.millis)

    writeMessages(5, 5, pId, writerUuid)

    probe
        .expectNextN(expectedEvents(pId, 5, 5))
        .expectNoMessage(300.millis)

    probe.cancel()
  }

  it should "fetch events after the current latest event" in {
    val pId = "ebp-b"
    val writerUuid = UUID.randomUUID()
    writeMessages(1, 3, pId, writerUuid)

    val probe = readJournal.eventsByPersistenceId(pId, 4, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(5)
        .expectNoMessage(300.millis)

    writeMessages(4, 5, pId, writerUuid)

    probe
        .expectNextN(expectedEvents(pId, 4, 5))
        .expectNoMessage(300.millis)

    probe.cancel()
  }

  it should "fetch a subset of events" in {
    val pId = "ebp-c"
    val writerUuid = UUID.randomUUID()
    writeMessages(1, 3, pId, writerUuid)

    val probe = readJournal.eventsByPersistenceId(pId, 1, 4)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(5)
        .expectNextN(expectedEvents(pId, 1, 3))
        .expectNoMessage(300.millis)

    writeMessages(4, 4, pId, writerUuid)

    probe
        .expectNextN(expectedEvents(pId, 4, 4))
        .expectComplete()
  }

  it should "fetch events after demand request" in {
    val pId = "ebp-d"
    val writerUuid = UUID.randomUUID()
    writeMessages(1, 3, pId, writerUuid)

    val probe = readJournal.eventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(2)
        .expectNextN(expectedEvents(pId, 1, 2))
        .expectNoMessage(300.millis)

    writeMessages(4, 4, pId, writerUuid)

    probe
        .request(5)
        .expectNextN(expectedEvents(pId, 3, 4))
        .expectNoMessage(300.millis)

    probe.cancel()
  }

  it should "only deliver what is requested even if there is more in the buffer" in {
    val pId = "ebp-e"
    val writerUuid = UUID.randomUUID()
    writeMessages(1, 10, pId, writerUuid)

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
    val pId = "ebp-f"

    val probe = readJournal.eventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .ensureSubscription()
        .request(10)
        .expectNoMessage(300.millis)

    probe.cancel()
  }

  it should "fetch events in the correct order" in {
    val pId = "ebp-g"
    val writerUuid = UUID.randomUUID()
    writeMessages(1, 3, pId, writerUuid)

    val events = readJournal.eventsByPersistenceId(pId, 0, Long.MaxValue)
        .map(envelope => (envelope.sequenceNr, envelope.event, envelope.offset.asInstanceOf[Sequence]))
        .runWith(TestSink.probe[(Long, Any, Sequence)])
        .request(3)
        .expectNextN(3)

    events(1)._3.value should be > events.head._3.value
    events(1)._3.value should be < events.last._3.value
  }

  private def expectedEvents(pId: String, fromSeq: Long, toSeq: Long): Seq[(Long, Any)] =
    for (i <- fromSeq to toSeq) yield (i, s"$pId-$i")

}
