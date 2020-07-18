package akka.persistence.r2dbc.journal

import io.r2dbc.spi.Result
import java.lang.{Long => JLong}
import org.reactivestreams.Publisher

private[akka] object ResultUtils {

  def toJournalEntry(result: Result): Publisher[JournalEntry] =
    result.map((row, _) => JournalEntry.of(
      row.get("id", classOf[JLong]),
      row.get("persistence_id", classOf[String]),
      row.get("sequence_nr", classOf[JLong]),
      row.get("payload", classOf[Array[Byte]]))
    )

  def toPersistenceId(result: Result): Publisher[(JLong, String)] =
    result.map((row, _) => (
        row.get("id", classOf[JLong]),
        row.get("persistence_id", classOf[String])
    ))

  def toSeqId(result: Result, name: String): Publisher[JLong] =
    result.map((row, _) => {
      val seq = row.get(name, classOf[JLong])
      if (seq == null) 0L else seq
    })

}
