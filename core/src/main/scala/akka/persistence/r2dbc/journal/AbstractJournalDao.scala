package akka.persistence.r2dbc.journal

import akka.NotUsed
import akka.stream.scaladsl.Source
import java.lang.{Integer => JInt, Long => JLong}
import java.util.{List => JList}
import scala.jdk.CollectionConverters._

/**
 * Java API.
 */
abstract class AbstractJournalDao extends JournalDao {

  def doWriteEvents(events: JList[JournalEntry]): Source[JInt, NotUsed]

  def doFetchEvents(persistenceId: String, fromSeqNr: JLong, toSeqNr: JLong, max: JLong): Source[JournalEntry, NotUsed]

  def doDeleteEvents(persistenceId: String, toSeqNr: JLong): Source[JInt, NotUsed]

  def doReadHighestSequenceNr(persistenceId: String, fromSeqNr: JLong): Source[JLong, NotUsed]

  final def writeEvents(events: Seq[JournalEntry]): Source[Int, NotUsed] =
    doWriteEvents(events.asJava).map(_.toInt)

  final def fetchEvents(persistenceId: String, fromSeqNr: Long, toSeqNr: Long, max: Long): Source[JournalEntry, NotUsed] =
    doFetchEvents(persistenceId, fromSeqNr, toSeqNr, max)

  final def deleteEvents(persistenceId: String, toSeqNr: Long): Source[Int, NotUsed] =
    doDeleteEvents(persistenceId, toSeqNr).map(_.toInt)

  final def readHighestSequenceNr(persistenceId: String, fromSeqNr: Long): Source[Long, NotUsed] =
    doReadHighestSequenceNr(persistenceId, fromSeqNr).map(_.toLong)

}
