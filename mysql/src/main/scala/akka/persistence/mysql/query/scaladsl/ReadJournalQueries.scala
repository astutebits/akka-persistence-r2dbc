package akka.persistence.mysql.query.scaladsl

import java.lang.{Long => JLong}

private[akka] object ReadJournalQueries {

  def fetchPersistenceIdsQuery(offset: JLong): String =
    "SELECT persistence_id, max(id) AS id FROM journal_event" +
        s" WHERE id >= $offset GROUP BY persistence_id ORDER BY id;"


  def fetchByPersistenceIdQuery(
      persistenceId: String,
      fromSeqNr: JLong,
      toSeqNr: JLong
  ): String =
    s"SELECT id, persistence_id, seq_nr, event FROM journal_event " +
        s" WHERE persistence_id = '$persistenceId'" +
        s" AND seq_nr >= $fromSeqNr AND seq_nr <= $toSeqNr" +
        s" ORDER BY seq_nr ASC"

  def fetchByTagQuery(tag: String, fromIndex: JLong, toIndex: JLong): String =
    s"SELECT j.id, j.persistence_id, j.seq_nr, j.event" +
        s" FROM journal_event j " +
        s" JOIN event_tag t ON j.id = t.journal_event_id " +
        s" WHERE t.tag = '$tag' AND j.id >= $fromIndex AND j.id <= $toIndex" +
        s" ORDER BY j.id ASC"

  def findHighestIndexQuery(tag: String): String =
    s"SELECT MAX(journal_event_id) AS journal_event_id FROM event_tag WHERE tag = '$tag'"

  def findHighestSeqQuery(persistenceId: String): String =
    s"SELECT max(seq_nr) AS seq_nr FROM journal_event WHERE persistence_id = '$persistenceId'"
}
