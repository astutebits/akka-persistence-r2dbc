# Akka Persistence R2DBC
An exploration of implementing the Akka [Journal][0] and [Snapshot store][1]
plugin APIs using [R2DBC][2].

[0]: https://doc.akka.io/docs/akka/current/persistence-journals.html#journal-plugin-api
[1]: https://doc.akka.io/docs/akka/current/persistence-journals.html#snapshot-store-plugin-api
[2]: https://r2dbc.io/

## Configuration
**Connection pool configuration**

| Option                       | Type     | Description
| ------                       | ----     | -----------
| `acquire-retry`              | Integer  | Number of retries if the first connection acquire attempt fails. Defaults to `1`.
| `initial-size`               | Integer  | Initial pool size. Defaults to `10`.
| `max-size`                   | Integer  | Maximum pool size. Defaults to `10`.
| `max-life-time`              | Duration | Maximum lifetime of the connection in the pool. Defaults to `no timeout`.
| `max-idle-time`              | Duration | Maximum idle time of the connection in the pool. Defaults to `30 minutes`.
| `max-acquire-time`           | Duration | Maximum time to acquire connection from pool. Defaults to `no timeout`.
| `max-create-connection-time` | Duration | Maximum time to create a new connection. Defaults to `no timeout`.
