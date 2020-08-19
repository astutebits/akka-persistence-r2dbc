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

package akka.persistence.r2dbc.query

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source
import java.lang.{Long => JLong}

/**
 * JAVA API for [[QueryDao]].
 */
abstract class AbstractQueryDao extends QueryDao {

  final override def fetchPersistenceIds(offset: Long): Source[(Long, String), NotUsed] =
    doFetchPersistenceIds(offset).map(x => x.copy(x._1.toLong))

  final override def fetchByPersistenceId(
      persistenceId: String,
      fromSeqNr: Long,
      toSeqNr: Long
  ): Source[JournalEntry, NotUsed] =
    doFetchByPersistenceId(persistenceId, fromSeqNr, toSeqNr)

  final override def fetchByTag(
      tag: String,
      fromIndex: Long,
      toIndex: Long
  ): Source[JournalEntry, NotUsed] =
    doFetchByTag(tag, fromIndex, toIndex)

  final override def findHighestIndex(tag: String): Source[Long, NotUsed] =
    doFindHighestIndex(tag).map(_.toLong)

  final override def findHighestSeq(persistenceId: String): Source[Long, NotUsed] =
    doFindHighestSeq(persistenceId).map(_.toLong)

  def doFetchPersistenceIds(offset: JLong): Source[(JLong, String), NotUsed]

  def doFetchByPersistenceId(
      persistenceId: String,
      fromSeqNr: JLong,
      toSeqNr: JLong
  ): Source[JournalEntry, NotUsed]

  def doFetchByTag(
      tag: String,
      fromSeqNr: JLong,
      toSeqNr: JLong
  ): Source[JournalEntry, NotUsed]

  def doFindHighestIndex(tag: String): Source[JLong, NotUsed]

  def doFindHighestSeq(query: String): Source[JLong, NotUsed]

}
