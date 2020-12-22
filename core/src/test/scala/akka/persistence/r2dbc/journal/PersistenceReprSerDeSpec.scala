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

package akka.persistence.r2dbc.journal

import akka.actor.{ActorSystem, ExtendedActorSystem}
import akka.persistence.PersistentRepr
import akka.persistence.journal.Tagged
import akka.persistence.r2dbc.journal.PersistenceReprSerDeSpec.{Message1, Test}
import akka.serialization.{Serialization, SerializerWithStringManifest}
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

import scala.collection.immutable


object PersistenceReprSerDeSpec {

  sealed trait Protocol

  case class Message1(str: String) extends Protocol

  case class Message2(str: String) extends Protocol

  class TestSerializer(val system: ExtendedActorSystem) extends SerializerWithStringManifest {
    override def identifier: Int = 10000

    override def manifest(o: AnyRef): String = o match {
      case _: Message1 => "1"
      case _: Message2 => "2"
    }

    override def toBinary(o: AnyRef): Array[Byte] = o match {
      case Message1(msg) => msg.getBytes
      case Message2(msg) => msg.getBytes
    }

    override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
      case "1" => Message1(new String(bytes))
      case "2" => Message2(new String(bytes))
    }
  }

  case object Test

}

/**
 * Test case for [[PersistenceReprSerDe]].
 */
final class PersistenceReprSerDeSpec
    extends AnyFlatSpecLike
        with Matchers {

  private val config = ConfigFactory.parseString(
    """
       akka {
        actor {
          serializers {
            serde = "akka.persistence.r2dbc.journal.PersistenceReprSerDeSpec$TestSerializer"
          }
          serialization-bindings = {
            "akka.persistence.r2dbc.journal.PersistenceReprSerDeSpec$Protocol" = serde
          }
        }
       }
    """)

  private val system = ActorSystem(getClass.getSimpleName, config).asInstanceOf[ExtendedActorSystem]

  private val serializer = new PersistenceReprSerDe(new Serialization(system))

  private def assertSerDe(repr: PersistentRepr, additionallyAssert: JournalEntry => Unit): Unit = {
    val serialized = serializer.serialize(repr) getOrElse fail(s"Cannot serialize $repr")
    val deserialized = serializer.deserialize(serialized) getOrElse fail(s"Cannot deserialize $serialized")

    assert(repr.persistenceId == serialized.persistenceId
        && serialized.persistenceId == deserialized.persistenceId)
    assert(repr.sequenceNr == serialized.sequenceNr
        && serialized.sequenceNr == deserialized.sequenceNr)
    assert(repr.manifest == serialized.eventManifest
        && serialized.eventManifest == deserialized.manifest)

    serialized.serId shouldBe 10000

    additionallyAssert(serialized)
  }

  "PersistenceReprSerDe" should "serialize and deserialize message" in {
    val persistentRepr = PersistentRepr(
      payload = Message1("abc"),
      sequenceNr = 123, persistenceId = "foo", manifest = "manifest"
    )
    assertSerDe(persistentRepr, it => {
      it.serManifest shouldBe "1"
      it.tags shouldBe immutable.Set.empty[String]
      it.projected shouldBe None
    })
  }

  it should "serialize and deserialize Tagged" in {
    val persistentRepr = PersistentRepr(
      payload = Tagged(Message1("abc"), immutable.Set("tag")),
      sequenceNr = 123, persistenceId = "foo", manifest = "manifest"
    )
    assertSerDe(persistentRepr, it => {
      it.serManifest shouldBe "1"
      it.tags shouldBe immutable.Set("tag")
      it.projected shouldBe None
    })
  }

  it should "serialize and deserialize Projected" in {
    val persistentRepr = PersistentRepr(
      payload = Projected(Message1("abc"), "INSERT INTO"),
      sequenceNr = 123, persistenceId = "foo", manifest = "manifest"
    )
    assertSerDe(persistentRepr, it => {
      it.serManifest shouldBe "1"
      it.tags shouldBe immutable.Set.empty[String]
      it.projected shouldBe Some("INSERT INTO")
    })
  }

  it should "serialize and deserialize Tagged(Projected)" in {
    val persistentRepr = PersistentRepr(
      payload = Tagged(Projected(Message1("abc"), "INSERT INTO"), immutable.Set("tag")),
      sequenceNr = 123, persistenceId = "foo", manifest = "manifest"
    )
    assertSerDe(persistentRepr, it => {
      it.serManifest shouldBe "1"
      it.tags shouldBe immutable.Set("tag")
      it.projected shouldBe Some("INSERT INTO")
    })
  }

  it should "serialize and deserialize Projected(Tagged)" in {
    val persistentRepr = PersistentRepr(
      payload = Projected(Tagged(Message1("abc"), immutable.Set("tag")), "INSERT INTO"),
      sequenceNr = 123, persistenceId = "foo", manifest = "manifest"
    )
    assertSerDe(persistentRepr, it => {
      it.serManifest shouldBe "1"
      it.tags shouldBe immutable.Set("tag")
      it.projected shouldBe Some("INSERT INTO")
    })
  }

  it should "catch serialization errors" in {
    val serialized = serializer.serialize(PersistentRepr(Test))
    serialized should be a Symbol("failure")
  }

}
