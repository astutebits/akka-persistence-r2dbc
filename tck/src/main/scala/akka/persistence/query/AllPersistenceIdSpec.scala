package akka.persistence.query

import akka.persistence.query.scaladsl.{CurrentPersistenceIdsQuery, PersistenceIdsQuery}
import akka.stream.testkit.scaladsl.TestSink
import java.util.UUID
import org.scalatest.BeforeAndAfterEach
import scala.concurrent.duration._

/**
 * Test case for [[CurrentPersistenceIdsQuery]] and [[PersistenceIdsQuery]].
 */
trait AllPersistenceIdSpec { _: ReadJournalSpec =>

  "CurrentPersistenceIdsQuery" should "return empty if there are none" in {
    readJournal.currentPersistenceIds()
        .runWith(TestSink.probe)
        .expectSubscriptionAndComplete()
  }

  it should "fetch all persistence ids" in {
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, "foo", writerUuid)
    writeMessages(1, 5, "bar", writerUuid)
    writeMessages(6, 10, "foo", writerUuid)
    writeMessages(6, 10, "bar", writerUuid)

    readJournal.currentPersistenceIds()
        .runWith(TestSink.probe)
        .request(10)
        .expectNext("foo", "bar")
        .expectComplete()
  }

  it should "not fetch persistence ids added after the stream started" in {
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, "foo", writerUuid)
    writeMessages(1, 5, "bar", writerUuid)
    writeMessages(6, 10, "foo", writerUuid)
    writeMessages(6, 10, "bar", writerUuid)

    val probe = readJournal.currentPersistenceIds()
        .runWith(TestSink.probe)
        .request(2)
        .expectNext("foo", "bar")

    writeMessages(1, 5, "baz", writerUuid)

    probe
        .request(5)
        .expectComplete()
  }

  it should "only fetch what is requested even if there is more in the buffer" in {
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, "foo", writerUuid)
    writeMessages(1, 5, "bar", writerUuid)
    writeMessages(6, 10, "foo", writerUuid)
    writeMessages(6, 10, "bar", writerUuid)
    writeMessages(1, 5, "baz", writerUuid)

    val probe = readJournal.currentPersistenceIds()
        .runWith(TestSink.probe)
        .request(2)
        .expectNext("foo", "bar")
        .expectNoMessage(100.millis)

    probe
        .request(1)
        .expectNext("baz")
        .expectComplete()
  }

  "PersistenceIdsQuery" should "keep running even if empty" in {
    readJournal.persistenceIds()
        .runWith(TestSink.probe)
        .request(100)
        .expectNoMessage(300.millis)
        .cancel()
  }

  it should "keep fetching persistence Ids" in {
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, "foo", writerUuid)
    writeMessages(1, 5, "bar", writerUuid)
    writeMessages(6, 10, "foo", writerUuid)
    writeMessages(6, 10, "bar", writerUuid)

    val probe = readJournal.persistenceIds()
        .runWith(TestSink.probe)
        .request(10)
        .expectNext("foo", "bar")
        .expectNoMessage(100.millis)

    writeMessages(1, 5, "baz", writerUuid)

    probe
        .expectNext("baz")
        .cancel()
  }

  it should "only fetch what is requested even if there is more in the buffer" in {
    val writerUuid = UUID.randomUUID()

    writeMessages(1, 5, "foo", writerUuid)
    writeMessages(1, 5, "bar", writerUuid)
    writeMessages(6, 10, "foo", writerUuid)
    writeMessages(6, 10, "bar", writerUuid)
    writeMessages(1, 5, "baz", writerUuid)

    val probe = readJournal.persistenceIds()
        .runWith(TestSink.probe)
        .request(2)
        .expectNext("foo", "bar")
        .expectNoMessage(100.millis)

    probe
        .request(1)
        .expectNext("baz")
        .cancel()
  }

}
