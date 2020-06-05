package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source

trait QueryDao {

  /**
   * Returns a [[Source]] with a subset of [[JournalEntry]] in the selected `sequenceNr` range for
   * the given `persistenceId`.
   *
   * @param persistenceId the persistence id.
   * @param fromSeqNr from sequence number (inclusive).
   * @param toSeqNr to sequence number (inclusive).
   * @return
   */
  def fetchByPersistenceId(
      persistenceId: String,
      fromSeqNr: Long,
      toSeqNr: Long
  ): Source[JournalEntry, NotUsed]

  /**
   * Returns a [[Source]] with [[JournalEntry]] with sequence numbers in the selected range for the
   * given `tag`.
   *
   * @param tag the tag
   * @param fromSeqNr from sequence number (inclusive)
   * @param toSeqNr to sequence number (inclusive)
   * @return
   */
  def fetchByTag(tag: String, fromSeqNr: Long, toSeqNr: Long): Source[JournalEntry, NotUsed]

  /**
   * Returns a single element [[Source]] with the highest index number for the given tag.
   *
   * @param tag the tag
   * @return a single element
   */
  def findHighestIndex(tag: String): Source[Long, NotUsed]

  /**
   * Returns a single element [[Source]] with the highest `sequenceNr` for the given `persistenceId`.
   *
   * @param persistenceId the persistence id.
   * @return a single element
   */
  def findHighestSeq(persistenceId: String): Source[Long, NotUsed]

}
