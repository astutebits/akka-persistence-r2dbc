package akka.persistence.r2dbc.journal

import akka.NotUsed
import akka.stream.scaladsl.Source

trait JournalDao {

  /**
   * Persists the journal events and any associated tags atomically and returns a single [[Unit]]
   * in the [[Source]].
   *
   * @param events the journal entries
   * @return a [[Source]] with a single [[Unit]]
   */
  def writeEvents(events: Seq[JournalEntry]): Source[Int, NotUsed]

  /**
   * Returns a [[Source]] with the journal event entries for the given `persistenceId` in the
   * `fromSeqNr` and `toSeqNr` range.
   *
   * @param persistenceId the persistence id
   * @param fromSeqNr sequence number where retrieve should start (inclusive).
   * @param toSeqNr sequence number where retrieve should end (inclusive).
   * @param max maximum number of messages to be returned.
   * @return a [[Source]] with the results
   */
  def fetchEvents(
      persistenceId: String,
      fromSeqNr: Long,
      toSeqNr: Long,
      max: Long
  ): Source[JournalEntry, NotUsed]

  /**
   * Deletes the journal events for the given persistent ID up to the provided sequence number
   * (inclusive).
   *
   * As required by the API, the message deletion doesn't affect the highest sequence number of
   * messages, and we maintain the highest sequence number by "marking" the journal event entry as
   * deleted. Instead of deleting it permanently.
   *
   * @param persistenceId the persistence id.
   * @param toSeqNr sequence number (inclusive).
   * @return a [[Source]] with a single [[Unit]]
   */

  def deleteEvents(persistenceId: String, toSeqNr: Long): Source[Int, NotUsed]

  /**
   * Returns a single [[Long]] in the [[Source]] with the highest sequence number for the given
   * `persistenceId`.
   *
   * @param persistenceId the persistence id.
   * @param fromSeqNr sequence number (inclusive).
   * @return a single entry [[Source]] with the highest sequence number, or empty if not found.
   */
  def readHighestSequenceNr(persistenceId: String, fromSeqNr: Long): Source[Long, NotUsed]

}
