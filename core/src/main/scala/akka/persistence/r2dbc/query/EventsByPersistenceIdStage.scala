package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source
import scala.concurrent.duration.FiniteDuration

private[akka] object EventsByPersistenceIdStage {

  def apply(
      dao: QueryDao,
      persistenceId: String,
      fromSeqNr: Long,
      toSeqNr: Long,
      refreshInterval: Option[FiniteDuration] = None
  ): EventsByPersistenceIdStage =
    new EventsByPersistenceIdStage(dao, persistenceId, fromSeqNr, toSeqNr, refreshInterval)

}

/**
 * Walks the journal entries returning any events that match the given persistence ID.
 */
private[akka] final class EventsByPersistenceIdStage private(
    dao: QueryDao,
    persistenceId: String,
    fromSeqNr: Long,
    toSeqNr: Long,
    val refreshInterval: Option[FiniteDuration] = None
) extends EventsByStage {

  require(dao != null, "the 'dao' must be provided")
  require(persistenceId != null && persistenceId.nonEmpty, "the 'persistenceId' must be provided")
  require(fromSeqNr >= 0, "the 'fromSeqNr' must be >= 0")
  require(toSeqNr >= 0, "the 'toSeqNr' must be >= 0")
  require(fromSeqNr < toSeqNr, "the 'fromSeqNr' must be < the 'toSeqNr'")

  private var currentSeq: Long = fromSeqNr
  private var targetSeq: Long = 0

  override protected def pushedEntry(entry: JournalEntry): Unit =
    currentSeq = entry.sequenceNr

  override protected def fetchEvents(): Source[JournalEntry, NotUsed] =
    dao.findHighestSeq(persistenceId)
        .flatMapConcat(result => {
          if (targetSeq == result)
            Source.empty
          else {
            targetSeq = if (result > toSeqNr && currentCycle == 0) toSeqNr else result
            dao.fetchByPersistenceId(persistenceId, if (currentCycle == 0) currentSeq else currentSeq + 1, targetSeq)
          }
        })

}
