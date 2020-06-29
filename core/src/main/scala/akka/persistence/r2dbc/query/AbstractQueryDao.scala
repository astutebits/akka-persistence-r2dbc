package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source
import java.lang.{Long => JLong}

/**
 * JAVA API for [[QueryDao]].
 */
abstract class AbstractQueryDao extends QueryDao {

  final override def fetchPersistenceIds(offset: Long): Source[(Long, String), NotUsed] =
    doFetchPersistenceIds(offset).map(x => x.copy(x._1.toLong))

  final override def fetchByPersistenceId(
      persistenceId: String,
      fromSeqNr: Long,
      toSeqNr: Long
  ): Source[JournalEntry, NotUsed] =
    doFetchByPersistenceId(persistenceId, fromSeqNr, toSeqNr)

  final override def fetchByTag(
      tag: String,
      fromIndex: Long,
      toIndex: Long
  ): Source[JournalEntry, NotUsed] =
    doFetchByTag(tag, fromIndex, toIndex)

  final override def findHighestIndex(tag: String): Source[Long, NotUsed] =
    doFindHighestIndex(tag).map(_.toLong)

  final override def findHighestSeq(persistenceId: String): Source[Long, NotUsed] =
    doFindHighestSeq(persistenceId).map(_.toLong)

  def doFetchPersistenceIds(offset: JLong): Source[(JLong, String), NotUsed]

  def doFetchByPersistenceId(
      persistenceId: String,
      fromSeqNr: JLong,
      toSeqNr: JLong
  ): Source[JournalEntry, NotUsed]

  def doFetchByTag(
      tag: String,
      fromSeqNr: JLong,
      toSeqNr: JLong
  ): Source[JournalEntry, NotUsed]

  def doFindHighestIndex(tag: String): Source[JLong, NotUsed]

  def doFindHighestSeq(query: String): Source[JLong, NotUsed]

}
