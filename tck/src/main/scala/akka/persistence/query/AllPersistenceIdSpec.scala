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

package akka.persistence.query

import akka.persistence.query.scaladsl.{CurrentPersistenceIdsQuery, PersistenceIdsQuery}
import akka.stream.testkit.scaladsl.TestSink
import java.util.UUID
import org.scalatest.BeforeAndAfterEach
import scala.concurrent.duration._

/**
 * Test case for [[CurrentPersistenceIdsQuery]] and [[PersistenceIdsQuery]].
 */
trait AllPersistenceIdSpec { _: ReadJournalSpec =>

  "CurrentPersistenceIdsQuery" should "fetch all persistence ids" in {
    val pIds = getAllPersistenceIds ++ List.fill(3)(persist(newPersistenceId, 5))

    readJournal.currentPersistenceIds()
        .runWith(TestSink.probe)
        .request(pIds.size * 2)
        .expectNextUnorderedN(pIds)
        .expectComplete()
  }

  it should "not fetch persistence ids added after the stream started" in {
   val pIds = getAllPersistenceIds ++ List.fill(3)(persist(newPersistenceId, 5))

    val probe = readJournal.currentPersistenceIds()
        .runWith(TestSink.probe)
        .request(pIds.size)
    .expectNextUnorderedN(pIds)

    persist(newPersistenceId, 5)

    probe
        .request(5)
        .expectComplete()
  }

  it should "only fetch what is requested even if there is more in the buffer" in {
    val pIds = getAllPersistenceIds ++ List.fill(3)(persist(newPersistenceId, 5))

    val probe = readJournal.currentPersistenceIds()
        .runWith(TestSink.probe)
        .request(pIds.size - 1)
        .expectNextUnorderedN(pIds.filterNot(x => x == pIds.last))
        .expectNoMessage(100.millis)

    probe
        .request(1)
        .expectNext(pIds.last)
        .expectComplete()
  }

  "PersistenceIdsQuery" should "keep fetching persistence Ids" in {
    val pIds = getAllPersistenceIds ++ List.fill(3)(persist(newPersistenceId, 5))

    val probe = readJournal.persistenceIds()
        .runWith(TestSink.probe)
        .request(pIds.size * 2)
        .expectNextUnorderedN(pIds)
        .expectNoMessage(100.millis)

    val pid = persist(newPersistenceId, 5)

    probe
        .expectNext(pid)
        .cancel()
  }

  it should "only fetch what is requested even if there is more in the buffer" in {
    val pIds = getAllPersistenceIds ++ List.fill(3)(persist(newPersistenceId, 5))

    val probe = readJournal.persistenceIds()
        .runWith(TestSink.probe)
        .request(pIds.size - 1)
        .expectNextUnorderedN(pIds.filterNot(x => x == pIds.last))
        .expectNoMessage(100.millis)

    probe
        .request(1)
        .expectNext(pIds.last)
        .cancel()
  }


}
