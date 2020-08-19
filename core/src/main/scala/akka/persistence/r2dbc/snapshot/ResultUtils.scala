/*
 * Copyright 2020 Borislav Borisov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
