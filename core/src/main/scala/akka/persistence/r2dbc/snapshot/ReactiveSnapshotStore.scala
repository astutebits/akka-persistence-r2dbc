package akka.persistence.r2dbc.snapshot

import akka.persistence.snapshot.SnapshotStore
import akka.serialization.SerializationExtension

/**
 * Base trait for SnapshotStore implementations that use Reactive Relational Database drivers.
 */
private[akka] trait ReactiveSnapshotStore extends SnapshotStore with SnapshotLogic {

  override final val serializer: SnapshotSerializer =
    SnapshotSerializerImpl(SerializationExtension(context.system))

}
