package akka.persistence.r2dbc.client

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import io.r2dbc.spi.test.{MockConnection, MockConnectionFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

final class R2dbcSpec extends AnyFlatSpecLike with Matchers with BeforeAndAfterAll {

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = Materializer(system)

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "R2dbc" should "open and close the connection if successful withHandle execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = new R2dbc(connectionFactory)

    r2dbc.withHandle(_ => Source(Seq(1, 2)))
        .runWith(TestSink.probe[Int])
        .requestNext(1)
        .requestNext(2)
        .expectComplete()

    connection.isCloseCalled shouldBe true
  }

  it should "open and close the connection if erroneous withHandle execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = new R2dbc(connectionFactory)

    r2dbc.withHandle(_ => Source.failed(new IllegalArgumentException("Boom")))
        .runWith(TestSink.probe[Int])
        .expectSubscriptionAndError()

    connection.isCloseCalled shouldBe true
  }

  it should "open and close the connection if successful inTransaction execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = new R2dbc(connectionFactory)

    r2dbc.inTransaction(_ => Source(Seq(1, 2)))
        .runWith(TestSink.probe[Int])
        .requestNext(1)
        .requestNext(2)
        .expectComplete()

    connection.isBeginTransactionCalled shouldBe true
    connection.isCommitTransactionCalled shouldBe true
    connection.isRollbackTransactionCalled shouldBe false
    connection.isCloseCalled shouldBe true
  }

  it should "open and close the connection if erroneous inTransaction execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = new R2dbc(connectionFactory)

    r2dbc.inTransaction(_ => Source.failed(new IllegalArgumentException("Boom")))
        .runWith(TestSink.probe[Int])
        .expectSubscriptionAndError()

    connection.isBeginTransactionCalled shouldBe true
    connection.isCommitTransactionCalled shouldBe false
    connection.isRollbackTransactionCalled shouldBe true
    connection.isCloseCalled shouldBe true
  }

}
