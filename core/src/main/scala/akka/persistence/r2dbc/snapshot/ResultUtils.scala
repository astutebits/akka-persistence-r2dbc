package akka.persistence.r2dbc.snapshot

import io.r2dbc.spi.Result
import java.lang.{Long => JLong}
import org.reactivestreams.Publisher

private[snapshot] object ResultUtils {

  def entryOf(result: Result): Publisher[SnapshotEntry] =
    result.map((row, _) => SnapshotEntry.of(
      row.get("persistence_id", classOf[String]),
      row.get("sequence_nr", classOf[JLong]),
      row.get("instant", classOf[JLong]),
      row.get("snapshot", classOf[Array[Byte]]))
    )

}
