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

package akka.persistence.r2dbc.journal

import io.r2dbc.spi.Result
import java.lang.{Integer => JInt, Long => JLong}
import org.reactivestreams.Publisher

private[akka] object ResultUtils {

  def toJournalEntry(result: Result): Publisher[JournalEntry] = {
    result.map((row, _) => JournalEntry.of(
      row.get("id", classOf[JLong]),
      row.get("persistence_id", classOf[String]),
      row.get("sequence_nr", classOf[JLong]),
      row.get("timestamp", classOf[JLong]),
      row.get("payload", classOf[Array[Byte]]),
      row.get("manifest", classOf[String]),
      row.get("ser_id", classOf[JInt]),
      row.get("ser_manifest", classOf[String]),
      row.get("writer_uuid", classOf[String])
    ))
  }

  def toPersistenceId(result: Result): Publisher[(Long, String)] = result.map((row, _) => {
    val id = row.get("id", classOf[JLong])
    (if (id == null) id else id.toLong, row.get("persistence_id", classOf[String]))
  })

  def toSeqId(result: Result, name: String): Publisher[Long] = result.map((row, _) => {
    val seq = row.get(name, classOf[JLong])
    if (seq == null) 0L else seq
  })

}
