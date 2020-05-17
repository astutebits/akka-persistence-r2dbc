package akka.persistence.r2dbc.journal

import akka.actor.ActorSystem
import akka.persistence.journal.Tagged
import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import java.util.concurrent.atomic.AtomicInteger
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.{AsyncIdiomaticMockito, ResetMocksAfterEachAsyncTest}
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, TryValues}
import scala.collection.immutable.Seq

final class ReactiveJournalSpec
    extends AsyncFlatSpecLike
        with AsyncIdiomaticMockito with ArgumentMatchersSugar
        with ResetMocksAfterEachAsyncTest
        with Matchers
        with TryValues
        with BeforeAndAfterAll {

  private val _system = ActorSystem(getClass.getSimpleName,
    ConfigFactory.parseString(
      """
        |akka.actor.allow-java-serialization = on
      """.stripMargin
    ).withFallback(ConfigFactory.defaultApplication()).resolve()
  )

  private val mockDao = mock[JournalDao]

  private val journal = new JournalLogic {
    override protected val dao: JournalDao = mockDao
    override implicit val system: ActorSystem = _system
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(_system)
    super.afterAll()
  }

  class Test

  "ReactiveJournal" should "write messages and return Nil when all successfully serialized" in {
    val writes = Seq(
      AtomicWrite(Seq(
        PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 1L, "foo"),
        PersistentRepr(Tagged("foo2", Set("FooTag", "FooTagTwo")), 2L, "foo")
      )),
      AtomicWrite(PersistentRepr(Tagged("bar", Set("BarTag")), 2L, "bar"))
    )
    mockDao.writeEvents(anySeq) returns Source(Seq(10))

    journal.asyncWriteMessages(writes) map { result =>
      mockDao.writeEvents(anySeq) was called

      result shouldBe Nil
    }
  }

  it should "only write serialized messages and return a Seq marking the failed ones" in {
    val writes = Seq(
      AtomicWrite(Seq(
        PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 1L, "foo"),
        PersistentRepr(Tagged("foo2", Set("FooTag", "FooTagTwo")), 2L, "foo")
      )),
      AtomicWrite(PersistentRepr(new Test, 1, "test")),
      AtomicWrite(PersistentRepr(Tagged("bar", Set("BarTag")), 2L, "bar"))
    )

    mockDao.writeEvents(anySeq) returns Source(Seq(10))

    journal.asyncWriteMessages(writes) map { result =>
      mockDao.writeEvents(anySeq) was called

      result.size shouldBe 3
      result.head should be a Symbol("success")
      result(1) should be a Symbol("failure")
      result.last should be a Symbol("success")
    }
  }

  it should "fail the future if it cannot write the messages" in {
    val writes = Seq(
      AtomicWrite(Seq(
        PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 1L, "foo"),
        PersistentRepr(Tagged("foo2", Set("FooTag", "FooTagTwo")), 2L, "foo")
      )),
      AtomicWrite(PersistentRepr(new Test, 1, "test")),
      AtomicWrite(PersistentRepr(Tagged("bar", Set("BarTag")), 2L, "bar"))
    )

    val exception = new IllegalArgumentException("Boom")
    mockDao.writeEvents(anySeq) returns Source.failed(exception)

    recoverToExceptionIf[IllegalArgumentException] {
      journal.asyncWriteMessages(writes)
    } map { result =>
      mockDao.writeEvents(anySeq) was called
      result shouldBe exception
    }
  }

  it should "return Success if the removal of the messages was successful" in {
    mockDao.deleteEvents("foo", 100) returns Source.single(100)

    journal.asyncDeleteMessagesTo("foo", 100) map { result =>
      mockDao.deleteEvents("foo", 100) was called
      result shouldBe()
    }
  }

  it should "return Failure if the removal of the messages failed" in {
    val exception = new IllegalArgumentException("Boom")
    mockDao.deleteEvents("foo", 100) returns Source.failed(exception)

    recoverToExceptionIf[IllegalArgumentException] {
      journal.asyncDeleteMessagesTo("foo", 100)
    } map { result =>
      mockDao.deleteEvents("foo", 100) was called
      result shouldBe exception
    }
  }

  it should "replay messages as requested" in {
    mockDao.fetchEvents("foo", 1, 2, 2) returns Source(
      Seq(journal.serializer.serialize(PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 1L, "foo")).map(_.copy(index = 1L)),
        journal.serializer.serialize(PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 2L, "foo")).map(_.copy(index = 5L)))
    )
        .map(_.get)

    val counter = new AtomicInteger(0)
    journal.asyncReplayMessages("foo", 1, 2, 2)(_ => counter.incrementAndGet()) map { result =>
      mockDao.fetchEvents("foo", 1, 2, 2) was called
      counter.get shouldBe 2

      result shouldBe()
    }
  }

  it should "return Failure if it cannot replay the messages due to a DB error" in {
    val exception = new IllegalArgumentException("Boom")
    mockDao.fetchEvents("foo", 1, 2, 2) returns Source.failed(exception)

    recoverToExceptionIf[IllegalArgumentException] {
      journal.asyncReplayMessages("foo", 1, 2, 2)(println)
    } map { result =>
      mockDao.fetchEvents("foo", 1, 2, 2) was called

      result shouldBe exception
    }
  }

  it should "return the highest sequence number" in {
    mockDao.readHighestSequenceNr("foo", 1) returns Source(Seq(1, 2))

    journal.asyncReadHighestSequenceNr("foo", 1) map { result =>
      mockDao.readHighestSequenceNr("foo", 1) was called
      result shouldBe 2
    }
  }

  it should "return 0 if the highest sequence number is nowhere to be found" in {
    mockDao.readHighestSequenceNr("foo", 1) returns Source.empty

    journal.asyncReadHighestSequenceNr("foo", 1) map { result =>
      mockDao.readHighestSequenceNr("foo", 1) was called
      result shouldBe 0
    }
  }

  it should "return Failure if the highest sequence number request fails due to a DB error" in {
    val exception = new IllegalArgumentException("Boom")
    mockDao.readHighestSequenceNr("foo", 1) returns Source.failed(exception)

    recoverToExceptionIf[IllegalArgumentException] {
      journal.asyncReadHighestSequenceNr("foo", 1)
    } map { result =>
      mockDao.readHighestSequenceNr("foo", 1) was called

      result shouldBe exception
    }
  }

}
