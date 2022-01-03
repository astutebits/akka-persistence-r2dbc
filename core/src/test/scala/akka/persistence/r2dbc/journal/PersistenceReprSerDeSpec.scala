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

package akka.persistence.r2dbc.journal

import akka.actor.{ ActorSystem, ExtendedActorSystem }
import akka.persistence.PersistentRepr
import akka.persistence.journal.Tagged
import akka.serialization.{
  AsyncSerializerWithStringManifest,
  Serialization,
  SerializationExtension,
  SerializerWithStringManifest
}
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.collection.immutable.Set
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object PersistenceReprSerDeSpec {

  private def verifyTransportInfo(system: ExtendedActorSystem): Unit = {
    Serialization.currentTransportInformation.value match {
      case null =>
        throw new IllegalStateException("currentTransportInformation was not set")
      case t =>
        if (t.system ne system)
          throw new IllegalStateException(s"wrong system in currentTransportInformation, ${t.system} != $system")
        if (t.address != system.provider.getDefaultAddress)
          throw new IllegalStateException(
            s"wrong address in currentTransportInformation, ${t.address} != ${system.provider.getDefaultAddress}")
    }
  }

  sealed trait Protocol
  sealed trait AsyncProtocol
  case class Message1(str: String) extends Protocol
  case class Message2(str: String) extends AsyncProtocol

  class TestSerializer(val system: ExtendedActorSystem) extends SerializerWithStringManifest {

    override def identifier: Int = 10000

    override def manifest(o: AnyRef): String = o match {
      case _: Message1 => "1"
    }

    override def toBinary(o: AnyRef): Array[Byte] = {
      verifyTransportInfo(system)
      o match {
        case Message1(msg) => msg.getBytes
      }
    }

    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
      verifyTransportInfo(system)
      manifest match {
        case "1" => Message1(new String(bytes))
      }
    }

  }
  class AsyncTestSerializer(val system: ExtendedActorSystem) extends AsyncSerializerWithStringManifest(system) {

    override def identifier: Int = 10001

    override def manifest(o: AnyRef): String = o match {
      case _: Message2 => "2"
    }

    override def toBinaryAsync(o: AnyRef): Future[Array[Byte]] = {
      verifyTransportInfo(system)
      Future {
        o match {
          case Message2(msg) => msg.getBytes
        }
      }(system.dispatcher)
    }

    override def fromBinaryAsync(bytes: Array[Byte], manifest: String): Future[AnyRef] = {
      verifyTransportInfo(system)
      Future {
        manifest match {
          case "2" => Message2(new String(bytes))
        }
      }(system.dispatcher)
    }

  }

  case object Test

}

/**
 * Test case for [[PersistenceReprSerDe]].
 */
final class PersistenceReprSerDeSpec extends AnyFlatSpecLike with Matchers {

  import PersistenceReprSerDeSpec._

  private val config = ConfigFactory.parseString("""
       akka {
        actor {
          serializers {
            serde = "akka.persistence.r2dbc.journal.PersistenceReprSerDeSpec$TestSerializer"
            async-serde = "akka.persistence.r2dbc.journal.PersistenceReprSerDeSpec$AsyncTestSerializer"
          }
          serialization-bindings = {
            "akka.persistence.r2dbc.journal.PersistenceReprSerDeSpec$Protocol" = serde
            "akka.persistence.r2dbc.journal.PersistenceReprSerDeSpec$AsyncProtocol" = async-serde
          }
        }
       }
    """)

  private val system = ActorSystem(getClass.getSimpleName, config).asInstanceOf[ExtendedActorSystem]

  private val asyncSerDe = new PersistenceReprSerDe(SerializationExtension(system))(system.dispatcher)

  private def assertSerDe(repr: PersistentRepr, additionallyAssert: JournalEntry => Unit): Unit = {
    val serialized = Await.result(asyncSerDe.serialize(repr), 1.second).getOrElse(fail(s"Cannot serialize $repr"))
    val deserialized =
      Await.result(asyncSerDe.deserialize(serialized), 1.second).getOrElse(fail(s"Cannot deserialize $serialized"))

    assert(
      repr.persistenceId == serialized.persistenceId
      && serialized.persistenceId == deserialized.persistenceId)
    assert(
      repr.sequenceNr == serialized.sequenceNr
      && serialized.sequenceNr == deserialized.sequenceNr)
    assert(
      repr.manifest == serialized.eventManifest
      && serialized.eventManifest == deserialized.manifest)

    additionallyAssert(serialized)
  }

  "PersistenceReprSerDe" should "serialize and deserialize message with Serializer implementation" in {
    val persistentRepr =
      PersistentRepr(payload = Message1("abc"), sequenceNr = 123, persistenceId = "foo", manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10000
        it.serManifest shouldBe "1"
        it.tags shouldBe Set.empty[String]
        it.projected shouldBe None
      })
  }

  it should "serialize and deserialize message with AsyncSerializer implementation" in {
    val persistentRepr =
      PersistentRepr(payload = Message2("abc"), sequenceNr = 123, persistenceId = "foo", manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10001
        it.serManifest shouldBe "2"
        it.tags shouldBe Set.empty[String]
        it.projected shouldBe None
      })
  }

  it should "serialize and deserialize Tagged with Serializer implementation" in {
    val persistentRepr = PersistentRepr(
      payload = Tagged(Message1("abc"), Set("tag")),
      sequenceNr = 123,
      persistenceId = "foo",
      manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10000
        it.serManifest shouldBe "1"
        it.tags shouldBe Set("tag")
        it.projected shouldBe None
      })
  }

  it should "serialize and deserialize Tagged with AsyncSerializer implementation" in {
    val persistentRepr = PersistentRepr(
      payload = Tagged(Message2("abc"), Set("tag")),
      sequenceNr = 123,
      persistenceId = "foo",
      manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10001
        it.serManifest shouldBe "2"
        it.tags shouldBe Set("tag")
        it.projected shouldBe None
      })
  }

  it should "serialize and deserialize Projected with Serializer implementation" in {
    val persistentRepr = PersistentRepr(
      payload = Projected(Message1("abc"), "INSERT INTO"),
      sequenceNr = 123,
      persistenceId = "foo",
      manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10000
        it.serManifest shouldBe "1"
        it.tags shouldBe Set.empty[String]
        it.projected shouldBe Some("INSERT INTO")
      })
  }

  it should "serialize and deserialize Projected with AsyncSerializer implementation" in {
    val persistentRepr = PersistentRepr(
      payload = Projected(Message2("abc"), "INSERT INTO"),
      sequenceNr = 123,
      persistenceId = "foo",
      manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10001
        it.serManifest shouldBe "2"
        it.tags shouldBe Set.empty[String]
        it.projected shouldBe Some("INSERT INTO")
      })
  }

  it should "serialize and deserialize Tagged(Projected) with Serializer implementation" in {
    val persistentRepr = PersistentRepr(
      payload = Tagged(Projected(Message1("abc"), "INSERT INTO"), Set("tag")),
      sequenceNr = 123,
      persistenceId = "foo",
      manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10000
        it.serManifest shouldBe "1"
        it.tags shouldBe Set("tag")
        it.projected shouldBe Some("INSERT INTO")
      })
  }

  it should "serialize and deserialize Tagged(Projected) with AsyncSerializer implementation" in {
    val persistentRepr = PersistentRepr(
      payload = Tagged(Projected(Message2("abc"), "INSERT INTO"), Set("tag")),
      sequenceNr = 123,
      persistenceId = "foo",
      manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10001
        it.serManifest shouldBe "2"
        it.tags shouldBe Set("tag")
        it.projected shouldBe Some("INSERT INTO")
      })
  }

  it should "serialize and deserialize Projected(Tagged) with Serializer implementation" in {
    val persistentRepr = PersistentRepr(
      payload = Projected(Tagged(Message1("abc"), Set("tag")), "INSERT INTO"),
      sequenceNr = 123,
      persistenceId = "foo",
      manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10000
        it.serManifest shouldBe "1"
        it.tags shouldBe Set("tag")
        it.projected shouldBe Some("INSERT INTO")
      })
  }

  it should "serialize and deserialize Projected(Tagged) with AsyncSerializer implementation" in {
    val persistentRepr = PersistentRepr(
      payload = Projected(Tagged(Message2("abc"), Set("tag")), "INSERT INTO"),
      sequenceNr = 123,
      persistenceId = "foo",
      manifest = "manifest")
    assertSerDe(
      persistentRepr,
      it => {
        it.serId shouldBe 10001
        it.serManifest shouldBe "2"
        it.tags shouldBe Set("tag")
        it.projected shouldBe Some("INSERT INTO")
      })
  }

  it should "catch serialization errors" in {
    val serialized = Await.result(asyncSerDe.serialize(PersistentRepr(Test)), 1.second)
    (serialized should be).a(Symbol("failure"))
  }

}
