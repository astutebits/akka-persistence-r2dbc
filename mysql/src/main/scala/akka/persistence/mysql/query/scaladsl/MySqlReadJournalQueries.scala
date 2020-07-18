package akka.persistence.mysql.query.scaladsl

import java.lang.{Long => JLong}

private[akka] object MySqlReadJournalQueries {

  def fetchPersistenceIdsQuery(offset: JLong): String =
    "SELECT persistence_id, max(id) AS id FROM event" +
        s" WHERE id >= $offset GROUP BY persistence_id ORDER BY id"

  def fetchByPersistenceIdQuery(
      persistenceId: String,
      fromSeqNr: JLong,
      toSeqNr: JLong
  ): String =
    s"SELECT id, persistence_id, sequence_nr, payload FROM event " +
        s" WHERE persistence_id = '$persistenceId'" +
        s" AND sequence_nr >= $fromSeqNr AND sequence_nr <= $toSeqNr" +
        s" ORDER BY sequence_nr ASC"

  def fetchByTagQuery(tag: String, fromIndex: JLong, toIndex: JLong): String =
    s"SELECT e.id, e.persistence_id, e.sequence_nr, e.payload" +
        s" FROM event e " +
        s" JOIN tag t ON e.id = t.event_id " +
        s" WHERE t.tag = '$tag' AND e.id >= $fromIndex AND e.id <= $toIndex" +
        s" ORDER BY e.id ASC"

  def findHighestIndexQuery(tag: String): String =
    s"SELECT MAX(event_id) AS event_id FROM tag WHERE tag = '$tag'"

  def findHighestSeqQuery(persistenceId: String): String =
    s"SELECT max(sequence_nr) AS sequence_nr FROM event WHERE persistence_id = '$persistenceId'"

}
