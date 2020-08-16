package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.FiniteDuration

private[query] object EventsByPersistenceIdStage {

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
private[query] final class EventsByPersistenceIdStage private(
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

  final protected val completeSwitch = new AtomicBoolean(false)

  private var processedEntries: Long = 0
  private var currentSeq: Long = fromSeqNr
  private var targetSeq: Long = 0

  override protected def pushedEntry(entry: JournalEntry): Unit = {
    processedEntries += 1
    currentSeq = entry.sequenceNr
  }

  override protected def fetchEvents(): Source[JournalEntry, NotUsed] =
    dao.findHighestSeq(persistenceId)
        .flatMapConcat(highestSeq => {
          if (targetSeq == highestSeq) {
            Source.empty
          } else {
            targetSeq = if (highestSeq >= toSeqNr) {
              completeSwitch.set(true)
              toSeqNr
            } else highestSeq

            dao.fetchByPersistenceId(persistenceId, if (processedEntries == 0) currentSeq else currentSeq + 1, targetSeq)
          }
        })

}
