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

import akka.NotUsed
import akka.stream.scaladsl.Source
import java.lang.{Integer => JInt, Long => JLong}
import java.util.{List => JList}
import scala.jdk.CollectionConverters._

/**
 * Java API.
 */
abstract class AbstractJournalDao extends JournalDao {

  def doWriteEvents(events: JList[JournalEntry]): Source[JInt, NotUsed]

  def doFetchEvents(persistenceId: String, fromSeqNr: JLong, toSeqNr: JLong, max: JLong): Source[JournalEntry, NotUsed]

  def doDeleteEvents(persistenceId: String, toSeqNr: JLong): Source[JInt, NotUsed]

  def doReadHighestSequenceNr(persistenceId: String, fromSeqNr: JLong): Source[JLong, NotUsed]

  final def writeEvents(events: Seq[JournalEntry]): Source[Int, NotUsed] =
    doWriteEvents(events.asJava).map(_.toInt)

  final def fetchEvents(persistenceId: String, fromSeqNr: Long, toSeqNr: Long, max: Long): Source[JournalEntry, NotUsed] =
    doFetchEvents(persistenceId, fromSeqNr, toSeqNr, max)

  final def deleteEvents(persistenceId: String, toSeqNr: Long): Source[Int, NotUsed] =
    doDeleteEvents(persistenceId, toSeqNr).map(_.toInt)

  final def readHighestSequenceNr(persistenceId: String, fromSeqNr: Long): Source[Long, NotUsed] =
    doReadHighestSequenceNr(persistenceId, fromSeqNr).map(_.toLong)

}
