package akka.persistence.r2dbc.journal

import akka.persistence.PersistentRepr
import akka.persistence.journal.Tagged
import akka.serialization.Serialization
import scala.collection.immutable.Set
import scala.util.Try

private[akka] object PersistenceReprSerializer {

  private def encodeTags(payload: Any): Set[String] = payload match {
    case Tagged(_, tags) => tags
    case _ => Set.empty
  }

}

private[akka] final class PersistenceReprSerializer(serialization: Serialization) {

  import PersistenceReprSerializer._

  def serialize(persistentRepr: PersistentRepr): Try[JournalEntry] = serialization
      .serialize(persistentRepr.payload match {
        case Tagged(payload, _) => persistentRepr.withPayload(payload)
        case _ => persistentRepr
      })
      .map(bytes =>
        JournalEntry(
          Long.MinValue,
          persistentRepr.persistenceId,
          persistentRepr.sequenceNr,
          bytes,
          encodeTags(persistentRepr.payload),
          persistentRepr.deleted
        )
      )

  def deserialize(entry: JournalEntry): Try[PersistentRepr] = serialization
      .deserialize(entry.event, classOf[PersistentRepr])

}
