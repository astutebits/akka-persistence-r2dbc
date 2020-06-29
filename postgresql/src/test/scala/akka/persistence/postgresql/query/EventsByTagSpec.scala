package akka.persistence.postgresql.query

import akka.persistence.query.scaladsl.{CurrentEventsByTagQuery, EventsByTagQuery}
import akka.persistence.query.{NoOffset, Sequence}
import akka.stream.testkit.scaladsl.TestSink
import java.util.UUID
import scala.concurrent.duration._

/**
 * Test case for [[CurrentEventsByTagQuery]] and [[EventsByTagQuery]].
 */
final class EventsByTagSpec extends PersistenceQuerySpec {

  "Postgresql query currentEventsByTag" should "implement standard CurrentEventsByTagQuery" in {
    readJournal.isInstanceOf[CurrentEventsByTagQuery] should ===(true)
  }

  it should "fetch all current events" in {
    val pId = "cebt-a"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 2, pId, writerUuid, Set(pId))
    writeMessages(3, 3, pId, writerUuid, Set(s"$pId-non"))
    writeMessages(4, 4, pId, writerUuid, Set(pId))

    readJournal.currentEventsByTag(pId, NoOffset)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .request(3)
        .expectNextN(expectedEvents(pId, 1, 2))
        .expectNextN(expectedEvents(pId, 4, 4))
        .expectComplete()
  }

  it should "fetch events starting at the given offset" in {
    val pId = "cebt-b"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, pId, writerUuid, Set(pId))

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
    val pId = "cebt-c"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 4, pId, writerUuid, Set(pId))

    readJournal.currentEventsByTag(pId, NoOffset)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])
        .request(4)

    writeMessages(5, 5, pId, writerUuid, Set(pId))
  }

  it should "not see any events if the stream starts after the highest offset" in {
    val pId = "cebt-d"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 4, pId, writerUuid, Set(pId))

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
    val pId = "cebt-e"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 4, pId, writerUuid, Set(pId))

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
    val pId = "cebt-f"

    readJournal.currentEventsByTag(pId, NoOffset)
        .runWith(TestSink.probe)
        .expectSubscriptionAndComplete()
  }

  "Postgresql query eventsByTag" should "implement standard EventsByTagQuery" in {
    readJournal.isInstanceOf[EventsByTagQuery] should ===(true)
  }

  it should "keep running even if the tag does not exist yet" in {
    val pId = "ebt-a"

    readJournal.eventsByTag(pId, NoOffset)
        .runWith(TestSink.probe)
        .request(1)
        .expectNoMessage(300.millis)
        .cancel()
  }

  it should "fetch all existing events and keep fetching new ones" in {
    val pId = "ebt-b"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 4, pId, writerUuid, Set(pId))

    val probe = readJournal.eventsByTag(pId, NoOffset)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])

    probe.request(5)
        .expectNextN(expectedEvents(pId, 1, 4))
        .expectNoMessage(300.millis)

    writeMessages(5, 5, pId, writerUuid, Set(pId))

    probe
        .expectNextN(expectedEvents(pId, 5, 5))
        .cancel()
  }

  it should "fetch events starting at the given offset" in {
    val pId = "ebt-c"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 4, pId, writerUuid, Set(pId))

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

    writeMessages(5, 5, pId, writerUuid, Set(pId))

    probe
        .expectNextN(expectedEvents(pId, 5, 5))
        .cancel()
  }

  it should "only fetch what is requested even if there is more in the buffer" in {
    val pId = "ebt-d"
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 4, pId, writerUuid, Set(pId))

    val probe = readJournal.eventsByTag(pId, NoOffset)
        .map(envelope => (envelope.sequenceNr, envelope.event))
        .runWith(TestSink.probe[(Long, Any)])

    probe.request(5)
        .expectNextN(expectedEvents(pId, 1, 4))
        .expectNoMessage(300.millis)

    writeMessages(5, 8, pId, writerUuid, Set(pId))

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
