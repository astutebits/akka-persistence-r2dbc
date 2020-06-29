package akka.persistence.postgresql.query

import java.lang.{Long => JLong}

private[akka] object QueryStatements {

  /*
  def fetchPersistenceIdsQuery(offset: JLong): String =
    s"SELECT DISTINCT ON (persistence_id) persistence_id, index FROM journal_event " +
        s" WHERE index >= $offset ORDER BY persistence_id,index DESC"
   */

  def fetchPersistenceIdsQuery(offset: JLong): String =
    s"SELECT persistence_id, max(index) AS index FROM journal_event" +
        s" WHERE index >= $offset GROUP BY persistence_id ORDER BY index;"

  def fetchPersistenceIdsQuery(fromIndex: JLong, toIndex: JLong): String =
    s"SELECT DISTINCT persistence_id FROM journal_event WHERE index >= $fromIndex AND index <= $toIndex"

  def fetchByPersistenceIdQuery(
      persistenceId: String,
      fromSeqNr: JLong,
      toSeqNr: JLong
  ): String =
    s"SELECT index, persistence_id, sequence_nr, event FROM journal_event " +
        s" WHERE persistence_id = '$persistenceId'" +
        s" AND sequence_nr >= $fromSeqNr AND sequence_nr <= $toSeqNr" +
        s" ORDER BY sequence_nr ASC"

  def fetchByTagQuery(tag: String, fromIndex: JLong, toIndex: JLong): String =
    s"SELECT j.index, j.persistence_id, j.sequence_nr, j.event" +
        s" FROM journal_event j " +
        s" JOIN tag t ON j.index = t.event_index " +
        s" WHERE t.tag = '$tag' AND j.index >= $fromIndex AND j.index <= $toIndex" +
        s" ORDER BY j.index ASC"

  def findHighestIndexQuery(): String =
    s"SELECT MAX(index) AS index FROM journal_event"

  def findHighestIndexQuery(tag: String): String =
    s"SELECT MAX(event_index) AS event_index FROM tag WHERE tag = '$tag'"

  def findHighestSeqQuery(persistenceId: String): String =
    s"SELECT max(sequence_nr) AS sequence_nr FROM journal_event WHERE persistence_id = '$persistenceId'"

}
