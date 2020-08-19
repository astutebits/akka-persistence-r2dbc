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
import akka.persistence.r2dbc.journal.PersistenceReprSerDeSpec.{Message1, Test}
import akka.serialization.{Serialization, SerializerWithStringManifest}
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers


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

  "PersistenceReprSerDe" should "serialize and deserialize entries" in {
    val persistentRepr = PersistentRepr(Message1("abc"), 123, "foo", "manifest")

    val serialized = serializer.serialize(persistentRepr)
    serialized should be a Symbol("success")

    val x = serialized.get
    x.persistenceId shouldBe "foo"
    x.sequenceNr shouldBe 123
    x.eventManifest shouldBe "manifest"
    x.serId shouldBe 10000
    x.serManifest shouldBe "1"

    val deserialized = serializer.deserialize(x)
    deserialized should be a Symbol("success")

    val y = deserialized.get
    y.persistenceId shouldBe "foo"
    y.sequenceNr shouldBe 123
    y.manifest shouldBe "manifest"
  }

  it should "catch serialization errors" in {
    val serialized = serializer.serialize(PersistentRepr(Test))
    serialized should be a Symbol("failure")
  }

}
