package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source

private[akka] trait QueryDao {

  /**
   * Returns a [[Source]] with a subset of [[JournalEntry]] in the selected `sequenceNr` range for
   * the given `persistenceId`.
   *
   * @param persistenceId the persistence id.
   * @param fromSeqNr from sequence number (inclusive).
   * @param toSeqNr to sequence number (inclusive).
   * @return a [[Source]] with [[JournalEntry]]s matching the criteria.
   */
  def fetchByPersistenceId(
      persistenceId: String,
      fromSeqNr: Long,
      toSeqNr: Long
  ): Source[JournalEntry, NotUsed]

  /**
   * Returns a [[Source]] with a subset of [[JournalEntry]] with record `index`es in the selected
   * range for the given `tag`.
   *
   * @param tag the tag.
   * @param fromIndex from index (inclusive).
   * @param toIndex to index (inclusive).
   * @return a [[Source]] with [[JournalEntry]]s matching the criteria.
   */
  def fetchByTag(tag: String, fromIndex: Long, toIndex: Long): Source[JournalEntry, NotUsed]

  /**
   * Returns a single element [[Source]] with the highest record index for the given `tag`, or 0 if
   * a record is not found.
   *
   * @param tag the tag.
   * @return a [[Source]] with a single [[Long]] element.
   */
  def findHighestIndex(tag: String): Source[Long, NotUsed]

  /**
   * Returns a single element [[Source]] with the highest `sequenceNr` for the given `persistenceId`,
   * or 0 if a record is not found.
   *
   * @param persistenceId the persistence id.
   * @return a [[Source]] with a single [[Long]] element.
   */
  def findHighestSeq(persistenceId: String): Source[Long, NotUsed]

}
