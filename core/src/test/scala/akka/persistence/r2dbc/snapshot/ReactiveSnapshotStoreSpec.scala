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

package akka.persistence.r2dbc.snapshot

import akka.actor.ActorSystem
import akka.persistence.{ SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria }
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import java.time.Instant
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.{ AsyncIdiomaticMockito, ResetMocksAfterEachAsyncTest }
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfterAll, TryValues }
import scala.util.Try

case class Counter(value: Int)

final class ReactiveSnapshotStoreSpec
    extends AsyncFlatSpecLike
    with AsyncIdiomaticMockito
    with ArgumentMatchersSugar
    with ResetMocksAfterEachAsyncTest
    with Matchers
    with TryValues
    with BeforeAndAfterAll {

  private val _system = ActorSystem(
    getClass.getSimpleName,
    ConfigFactory
      .parseString("""
          |akka.actor.allow-java-serialization = on
      """.stripMargin)
      .withFallback(ConfigFactory.defaultApplication())
      .resolve())

  private val mockDao = mock[SnapshotStoreDao]
  private val mockSerializer = mock[SnapshotSerializer]

  private val store = new SnapshotLogic {
    implicit override val system: ActorSystem = _system
    override val dao: SnapshotStoreDao = mockDao
    override protected val serializer: SnapshotSerializer = mockSerializer
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(_system)
    super.afterAll()
  }

  "ReactiveSnapshotStore" should "write snapshot entry and complete the future successfully" in {
    val (metadata, snapshot) = (SnapshotMetadata("foo", 1, Instant.now.getEpochSecond), "woohoo")
    val entry = SnapshotEntry(metadata.persistenceId, metadata.sequenceNr, metadata.timestamp, snapshot.getBytes)

    mockSerializer.serialize(metadata, snapshot).returns(Try(entry))
    mockDao.save(entry).returns(Source.single(1))

    store.saveAsync(metadata, snapshot).map { result =>
      mockSerializer.serialize(metadata, snapshot).was(called)
      mockDao.save(entry).was(called)
      result shouldBe ((): Unit)
    }
  }

  it should "fail the 'saveAsync' future if it cannot serialize the snapshot entry" in {
    val (metadata, snapshot) = (SnapshotMetadata("foo", 1, Instant.now.getEpochSecond), "oh, no")
    val cause = new IllegalStateException("Boom")

    mockSerializer.serialize(metadata, snapshot).returns(Try(throw cause))

    recoverToExceptionIf[IllegalStateException] {
      store.saveAsync(metadata, snapshot)
    }.map { exception =>
      mockSerializer.serialize(metadata, snapshot).wasCalled(once)
      mockDao.save(any).wasNever(called)
      exception shouldBe cause
    }
  }

  it should "fail the 'saveAsync' future if it cannot write the snapshot entry" in {
    val (metadata, snapshot) = (SnapshotMetadata("foo", 1, Instant.now.getEpochSecond), "hooray")
    val entry = SnapshotEntry(metadata.persistenceId, metadata.sequenceNr, metadata.timestamp, snapshot.getBytes)
    val exception = new IllegalStateException("Boom")

    mockSerializer.serialize(metadata, snapshot).returns(Try(entry))
    mockDao.save(entry).returns(Source.failed(exception))

    recoverToExceptionIf[IllegalStateException] {
      store.saveAsync(metadata, snapshot)
    }.map { result =>
      mockSerializer.serialize(metadata, snapshot).was(called)
      mockDao.save(entry).was(called)

      result shouldBe exception
    }
  }

  it should "load the snapshot that matches the criteria" in {
    val (metadata, snapshot) = (SnapshotMetadata("foo", 1, Instant.now.getEpochSecond), "meow, meow")
    val entry = SnapshotEntry(metadata.persistenceId, metadata.sequenceNr, metadata.timestamp, snapshot.getBytes)

    mockDao.fetchSnapshot("foo", SnapshotSelectionCriteria()).returns(Source.single(entry))
    mockSerializer.deserialize(entry).returns(Try(SelectedSnapshot(metadata, snapshot)))

    store.loadAsync("foo", SnapshotSelectionCriteria()).map { result =>
      mockDao.fetchSnapshot("foo", SnapshotSelectionCriteria()).wasCalled(once)
      mockSerializer.deserialize(entry).wasCalled(once)
      result shouldBe Some(SelectedSnapshot(metadata, snapshot))
    }
  }

  it should "complete the 'loadAsync' with None if a snapshot entry does not match the criteria" in {
    mockDao.fetchSnapshot("foo", SnapshotSelectionCriteria()).returns(Source.empty)

    store.loadAsync("foo", SnapshotSelectionCriteria()).map { result =>
      mockDao.fetchSnapshot("foo", SnapshotSelectionCriteria()).wasCalled(once)
      result shouldBe None
    }
  }

  it should "fail the 'loadAsync' future if the fetchSnapshot DAO call fails" in {
    val cause = new IllegalStateException("Boom")
    mockDao.fetchSnapshot("foo", SnapshotSelectionCriteria()).returns(Source.failed(cause))

    recoverToExceptionIf[IllegalStateException] {
      store.loadAsync("foo", SnapshotSelectionCriteria())
    }.map { exception =>
      mockDao.fetchSnapshot("foo", SnapshotSelectionCriteria()).wasCalled(once)
      exception shouldBe cause
    }
  }

  it should "fail the 'loadAsync' future if it cannot deserialize the snapshot entry" in {
    val (metadata, snapshot) = (SnapshotMetadata("foo", 1, Instant.now.getEpochSecond), "woof woof")
    val entry = SnapshotEntry(metadata.persistenceId, metadata.sequenceNr, metadata.timestamp, snapshot.getBytes)
    val cause = new IllegalStateException("Boom")

    mockDao.fetchSnapshot("foo", SnapshotSelectionCriteria()).returns(Source.single(entry))
    mockSerializer.deserialize(entry).returns(Try(throw cause))

    recoverToExceptionIf[IllegalStateException] {
      store.loadAsync("foo", SnapshotSelectionCriteria())
    }.map { exception =>
      mockDao.fetchSnapshot("foo", SnapshotSelectionCriteria()).wasCalled(once)
      mockSerializer.deserialize(entry).wasCalled(once)
      exception shouldBe cause
    }
  }

  it should "delete the snapshots after the seqNr for the given persistence ID" in {
    mockDao.deleteSnapshot("foo", 3).returns(Source.single(3))

    store.deleteAsync(SnapshotMetadata("foo", 3)).map { result =>
      mockDao.deleteSnapshot("foo", 3).wasCalled(once)
      result shouldBe ((): Unit)
    }
  }

  it should "fail the 'deleteAsync metadata' future when the DAO call fails" in {
    val cause = new IllegalStateException("Boom")
    mockDao.deleteSnapshot("foo", 3).returns(Source.failed(cause))

    recoverToExceptionIf[IllegalStateException] {
      store.deleteAsync(SnapshotMetadata("foo", 3))
    }.map { exception =>
      mockDao.deleteSnapshot("foo", 3).wasCalled(once)
      exception shouldBe cause
    }
  }

  it should "delete the snapshots for the given persistence id that match the search criteria" in {
    mockDao.deleteSnapshot("foo", SnapshotSelectionCriteria(3)).returns(Source.single(3))

    store.deleteAsync("foo", SnapshotSelectionCriteria(3)).map { result =>
      mockDao.deleteSnapshot("foo", SnapshotSelectionCriteria(3)).wasCalled(once)
      result shouldBe ((): Unit)
    }
  }

  it should "fail the 'deleteAsync section criteria' future when the DAO cll fails" in {
    val cause = new IllegalStateException("Boom")
    mockDao.deleteSnapshot("foo", SnapshotSelectionCriteria(3)).returns(Source.failed(cause))

    recoverToExceptionIf[IllegalStateException] {
      store.deleteAsync("foo", SnapshotSelectionCriteria(3))
    }.map { exception =>
      mockDao.deleteSnapshot("foo", SnapshotSelectionCriteria(3)).wasCalled(once)
      exception shouldBe cause
    }
  }

}
