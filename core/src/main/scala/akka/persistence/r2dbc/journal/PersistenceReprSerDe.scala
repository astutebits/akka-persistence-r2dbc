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

import akka.persistence.PersistentRepr
import akka.persistence.journal.Tagged
import akka.serialization.{Serialization, Serializers}
import scala.collection.immutable.Set
import scala.util.Try

private[akka] object PersistenceReprSerDe {

  private def encodeTags(payload: Any): Set[String] = payload match {
    case Tagged(_, tags) => tags
    case _ => Set.empty
  }

}

private[akka] final class PersistenceReprSerDe(serialization: Serialization) {

  import PersistenceReprSerDe._

  def serialize(persistentRepr: PersistentRepr): Try[JournalEntry] = Try {
    val event = (persistentRepr.payload match {
      case Tagged(payload, _) => persistentRepr.withPayload(payload)
      case _ => persistentRepr
    }).payload.asInstanceOf[AnyRef]

    val serializer = serialization.findSerializerFor(event)
    val manifest = Serializers.manifestFor(serializer, event)

    (event, serializer.identifier, manifest)
  } flatMap { case (event, serId, serManifest) =>
    serialization.serialize(event).map(bytes => JournalEntry(
      Long.MinValue,
      persistentRepr.persistenceId,
      persistentRepr.sequenceNr,
      bytes,
      persistentRepr.writerUuid,
      persistentRepr.manifest,
      persistentRepr.timestamp,
      serId,
      serManifest,
      encodeTags(persistentRepr.payload),
      persistentRepr.deleted
    ))
  }

  def deserialize(entry: JournalEntry): Try[PersistentRepr] =
    serialization
        .deserialize(entry.event, entry.serId, entry.serManifest)
        .map(payload => PersistentRepr(
          payload,
          persistenceId = entry.persistenceId,
          sequenceNr = entry.sequenceNr,
          manifest = entry.eventManifest,
          writerUuid = entry.writerUuid
        ))

}
