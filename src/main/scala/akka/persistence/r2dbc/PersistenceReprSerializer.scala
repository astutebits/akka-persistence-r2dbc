package akka.persistence.r2dbc

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
      .serialize(persistentRepr)
      .map(bytes => {
        JournalEntry(
          Long.MinValue,
          persistentRepr.deleted,
          persistentRepr.persistenceId,
          persistentRepr.sequenceNr,
          bytes,
          encodeTags(persistentRepr.payload))
      })

  def deserialize(event: JournalEntry): Try[PersistentRepr] = serialization
      .deserialize(event.message, classOf[PersistentRepr])

}
