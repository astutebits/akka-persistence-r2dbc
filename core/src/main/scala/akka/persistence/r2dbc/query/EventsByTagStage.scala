/*
 * Copyright 2020-2021 Borislav Borisov.
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

package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.duration.FiniteDuration

private[query] object EventsByTagStage {

  def apply(
      dao: QueryDao,
      tag: String,
      offset: Long,
      refreshInterval: Option[FiniteDuration] = None
  ): EventsByTagStage = new EventsByTagStage(dao, tag, offset, refreshInterval)

}

/**
 * Walks the journal entries returning any events that match the given tag.
 */
private[query] final class EventsByTagStage private(
    dao: QueryDao,
    tag: String,
    offset: Long,
    val refreshInterval: Option[FiniteDuration] = None
) extends EventsByStage {

  require(dao != null, "the 'dao' must be provided")
  require(tag != null && tag.nonEmpty, "the 'tag' must be provided")
  require(offset >= 0, "the 'offset' must be >= 0")

  private var processedEntries: Long = 0
  private var currentIndex: Long = offset
  private var targetIndex: Long = 0

  final protected val completeSwitch = new AtomicBoolean()

  override protected def pushedEntry(entry: JournalEntry): Unit = {
    processedEntries += 1
    currentIndex = entry.id
  }

  override protected def fetchEvents(): Source[JournalEntry, NotUsed] =
    dao.findHighestIndex(tag)
        .flatMapConcat(result => {
          if (targetIndex == result) {
            Source.empty
          } else {
            targetIndex = result
            dao.fetchByTag(tag, if (processedEntries == 0) currentIndex else currentIndex + 1, targetIndex)
          }
        })

}
