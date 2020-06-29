package akka.persistence.postgresql.query

import akka.actor.ExtendedActorSystem
import akka.persistence.query.ReadJournalProvider
import com.typesafe.config.Config

class PostgresqlReadJournalProvider (system: ExtendedActorSystem, config: Config) extends ReadJournalProvider {

  override val scaladslReadJournal: scaladsl.PostgresqlReadJournal =
    new scaladsl.PostgresqlReadJournal(system, config)

  override val javadslReadJournal: javadsl.PostgresqlReadJournal =
    new javadsl.PostgresqlReadJournal(scaladslReadJournal)

}
