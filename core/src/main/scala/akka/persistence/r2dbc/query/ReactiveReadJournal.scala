package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.actor.ExtendedActorSystem
import akka.persistence.query.scaladsl._
import akka.persistence.query.{EventEnvelope, NoOffset, Offset, Sequence}
import akka.persistence.r2dbc.journal.{JournalEntry, PersistenceReprSerializer}
import akka.serialization.SerializationExtension
import akka.stream.scaladsl.Source
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

trait ReactiveReadJournal
    extends ReadJournal
        with CurrentPersistenceIdsQuery
        with PersistenceIdsQuery
        with CurrentEventsByPersistenceIdQuery
        with EventsByPersistenceIdQuery
        with CurrentEventsByTagQuery
        with EventsByTagQuery {

  protected val system: ExtendedActorSystem

  private lazy val serializer = new PersistenceReprSerializer(SerializationExtension(system))
  protected val dao: QueryDao

  override def currentPersistenceIds(): Source[String, NotUsed] =
    Source.fromGraph(AllPersistentIdStage(dao))

  override def persistenceIds(): Source[String, NotUsed] =
    Source.fromGraph(AllPersistentIdStage(dao, Some(100.millis)))

  override def currentEventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long
  ): Source[EventEnvelope, NotUsed] =
    eventsByPersistenceIdInternal(persistenceId, fromSequenceNr, toSequenceNr)

  override def eventsByPersistenceId(persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long
  ): Source[EventEnvelope, NotUsed] =
    eventsByPersistenceIdInternal(persistenceId, fromSequenceNr, toSequenceNr, Some(100.millis))

  override def currentEventsByTag(tag: String, offset: Offset): Source[EventEnvelope, NotUsed] =
    eventsByTagInternal(tag, offset, None)

  override def eventsByTag(tag: String, offset: Offset): Source[EventEnvelope, NotUsed] =
    eventsByTagInternal(tag, offset, Some(100.millis))

  private[this] def eventsByPersistenceIdInternal(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      refreshInterval: Option[FiniteDuration] = None
  ): Source[EventEnvelope, NotUsed] = mapEntries(
    Source.fromGraph(EventsByPersistenceIdStage(dao, persistenceId, fromSequenceNr, toSequenceNr, refreshInterval))
  )

  private[this] def eventsByTagInternal(
      tag: String,
      offset: Offset,
      refreshInterval: Option[FiniteDuration]
  ): Source[EventEnvelope, NotUsed] = mapEntries(
    offset match {
      case Sequence(value) => Source.fromGraph(EventsByTagStage(dao, tag, value, refreshInterval))
      case NoOffset => Source.fromGraph(EventsByTagStage(dao, tag, 0, refreshInterval))
      case _ => Source.failed(new IllegalArgumentException("Only Sequence is supported"))
    }
  )

  private[this] def mapEntries(source: Source[JournalEntry, NotUsed]): Source[EventEnvelope, NotUsed] =
    source
        .map(entry => serializer.deserialize(entry).map((entry.index, _)))
        .mapAsync(1)(Future.fromTry)
        .map {
          case (index, repr) =>
            EventEnvelope(Sequence(index), repr.persistenceId, repr.sequenceNr, repr.payload, repr.timestamp)
        }

}
