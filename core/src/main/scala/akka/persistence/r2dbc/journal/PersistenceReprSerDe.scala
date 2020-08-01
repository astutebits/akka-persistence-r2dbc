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
