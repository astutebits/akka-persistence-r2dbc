package akka.persistence.mysql.query

import akka.actor.ExtendedActorSystem
import akka.persistence.query.ReadJournalProvider
import com.typesafe.config.Config

private[akka] final class MySqlReadJournalProvider(system: ExtendedActorSystem, config: Config)
    extends ReadJournalProvider {

  override val scaladslReadJournal: scaladsl.MySqlReadJournal =
    new scaladsl.MySqlReadJournal(system, config)

  override val javadslReadJournal: javadsl.MySqlReadJournal =
    new javadsl.MySqlReadJournal(scaladslReadJournal)

}
