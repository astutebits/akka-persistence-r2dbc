package akka.persistence.postgresql.journal

import akka.NotUsed
import akka.persistence.postgresql.journal.LenientPostgreSqlJournalDao.EntryTags
import akka.persistence.postgresql.journal.StrictPostgreSqlJournalDao.{findEventsQuery, highestSeqNrQuery, insertEntriesQuery, insertTagsQuery}
import akka.persistence.r2dbc.client.{Handle, R2dbc}
import akka.persistence.r2dbc.journal.ResultUtils.{toJournalEntry, toSeqId}
import akka.persistence.r2dbc.journal.{JournalDao, JournalEntry}
import akka.stream.scaladsl.Source
import io.r2dbc.spi.Result
import reactor.core.publisher.Flux

import scala.collection.JavaConverters._

private[journal] object StrictPostgreSqlJournalDao {

  val insertEntriesQuery: String = "INSERT INTO event (id, persistence_id, sequence_nr, timestamp, payload, manifest, " +
      "ser_id, ser_manifest, writer_uuid) VALUES (DEFAULT, $1, $2, $3, $4, $5, $6, $7, $8)"

  val insertTagsQuery: String = "INSERT INTO tag (id, event_id, tag) VALUES (DEFAULT, $1, $2)"

  val findEventsQuery = "SELECT id, persistence_id, sequence_nr, timestamp, payload, manifest, ser_id, ser_manifest, writer_uuid FROM event" +
      " WHERE deleted = false AND persistence_id = $1" +
      " AND sequence_nr BETWEEN $2 AND $3 ORDER BY sequence_nr ASC LIMIT $4"

  val highestSeqNrQuery: String = "SELECT sequence_nr FROM event WHERE persistence_id = $1" +
      " AND sequence_nr >= $2 ORDER BY sequence_nr DESC LIMIT 1"
}

final class StrictPostgreSqlJournalDao(val r2dbc: R2dbc) extends JournalDao {

  val lenient = new LenientPostgreSqlJournalDao(r2dbc)

  override def writeEvents(entries: Seq[JournalEntry]): Source[Int, NotUsed] = {
    val flux: Flux[Integer] = r2dbc.inTransaction((handle: Handle) => {
      val projections = entries.flatMap(it => it.projected).map(it => {
        handle.executeQuery(it._1, Seq(it._2), _.getRowsUpdated)
      })
      val events = entries.map(it => Array(it.persistenceId, it.sequenceNr, it.timestamp, it.event,
        it.eventManifest, it.serId, it.serManifest, it.writerUuid))

      Flux.concat(projections.asJava)
          .thenMany(handle.executeQuery(insertEntriesQuery, events, toSeqId(_, "id")))
          .zipWithIterable(entries.flatMap(it => Set(it.tags)).asJava)
          .collectList
          .filter((it: EntryTags) => it.asScala.map(_.getT2).exists(_.nonEmpty))
          .flatMapMany((eventTags: EntryTags) => {
            val tags = eventTags.asScala.flatMap(it => it.getT2.map(Array(it.getT1, _))).toSeq
            handle.executeQuery(insertTagsQuery, tags, _.getRowsUpdated)
          })
    })
    Source.fromPublisher(flux.defaultIfEmpty(0)).map(_.toInt)
  }

  override def fetchEvents(persistenceId: String, fromSeqNr: Long, toSeqNr: Long, max: Long): Source[JournalEntry, NotUsed] = {
    val flux = r2dbc.withHandle { handle =>
      handle.executeQuery(findEventsQuery, Seq(Array(persistenceId, fromSeqNr, toSeqNr, max)), toJournalEntry(_))
    }
    Source.fromPublisher(flux)
  }

  override def deleteEvents(persistenceId: String, toSeqNr: Long): Source[Int, NotUsed] = {
    lenient.deleteEvents(persistenceId, toSeqNr)
  }

  override def readHighestSequenceNr(persistenceId: String, fromSeqNr: Long): Source[Long, NotUsed] = {
    val flux = r2dbc.withHandle { handle =>
      val toSeqNr = (result: Result) => toSeqId(result, "sequence_nr")
      handle.executeQuery(highestSeqNrQuery, Seq(Array(persistenceId, fromSeqNr)), toSeqNr)
    }
    Source.fromPublisher(flux)
  }
}
