package akka.persistence.r2dbc.journal

import akka.persistence.journal.AsyncWriteJournal

/**
 * Base trait for journal implementations that use Reactive Relational Database drivers.
 */
trait ReactiveJournal extends AsyncWriteJournal with JournalLogic {

}
