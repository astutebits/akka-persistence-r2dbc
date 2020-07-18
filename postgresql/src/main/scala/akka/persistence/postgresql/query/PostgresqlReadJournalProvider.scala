package akka.persistence.postgresql.query

import akka.actor.ExtendedActorSystem
import akka.persistence.query.ReadJournalProvider
import com.typesafe.config.Config

private[akka] final class PostgresqlReadJournalProvider(system: ExtendedActorSystem, config: Config)
    extends ReadJournalProvider {

  override val scaladslReadJournal: scaladsl.PostgreSqlReadJournal =
    new scaladsl.PostgreSqlReadJournal(system, config)

  override val javadslReadJournal: javadsl.PostgreSqlReadJournal =
    new javadsl.PostgreSqlReadJournal(scaladslReadJournal)

}
