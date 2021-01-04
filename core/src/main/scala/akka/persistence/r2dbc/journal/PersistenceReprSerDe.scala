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

import akka.persistence.PersistentRepr
import akka.persistence.journal.Tagged
import akka.serialization.{AsyncSerializer, Serialization, Serializers}
import scala.collection.immutable.Set
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


private[akka] final class PersistenceReprSerDe(serialization: Serialization)(implicit ec: ExecutionContext) {

  def serialize(repr: PersistentRepr): Future[Try[JournalEntry]] = TryUtil.futureTry(() => Future {
    val (event: AnyRef, tags: Set[String], projection: Option[String]) = repr.payload match {
      case Projected(Tagged(payload, tags), projection) => (payload, tags, Some(projection))
      case Projected(payload, projection) => (payload, Set.empty, Some(projection))
      case Tagged(Projected(payload, projection), tags) => (payload, tags, Some(projection))
      case Tagged(payload, tags) => (payload, tags, None)
      case _ => (repr.payload, Set.empty, None)
    }

    val serializer = serialization.findSerializerFor(event)
    val manifest = Serializers.manifestFor(serializer, event)

    (event, tags, projection, serializer, manifest)
  } flatMap { case (event, tags, projection, serializer, manifest) =>
    val future = serializer match {
      case asyncSer: AsyncSerializer => withTransportInformation(() => asyncSer.toBinaryAsync(event))
      case sync => Future(withTransportInformation(() => sync.toBinary(event)))
    }
    future.map(JournalEntry(repr, serializer.identifier, manifest, _, tags, projection))
  })

  def deserialize(entry: JournalEntry): Future[Try[PersistentRepr]] = {
    val deserialized = serialization.serializerByIdentity.get(entry.serId) match {
      case Some(asyncSer: AsyncSerializer) => TryUtil.futureTry(() =>
        withTransportInformation(() => asyncSer.fromBinaryAsync(entry.event, entry.serManifest))
      )
      case _ => Future(serialization.deserialize(entry.event, entry.serId, entry.serManifest))
    }

    deserialized map { payload =>
      payload.map(pr =>
        PersistentRepr(
          pr,
          persistenceId = entry.persistenceId,
          sequenceNr = entry.sequenceNr,
          manifest = entry.eventManifest,
          writerUuid = entry.writerUuid
        ))
    }
  }

  private def withTransportInformation[T](f: () => T): T = {
    Serialization.withTransportInformation(serialization.system)(f)
  }

}
