# Akka Persistence R2DBC
An implementation of the Akka [Journal][0] and [Snapshot store][1] persistence
plugin APIs, and the full [Persistence Query][2] API with [R2DBC][3].

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

## Contributing
Contributions via GitHub pull requests are gladly accepted from their original
author. Along with any pull requests, please state that the contribution is
your original work and that you license the work to the project under the
project’s open source license. Whether or not you state this explicitly, by
submitting any copyrighted material via pull request, email, or other means you
agree to license the material under the project’s open source license and
warrant that you have the legal authority to do so.

## License
This project is licensed under the Apache 2.0 license.

[0]: https://doc.akka.io/docs/akka/current/persistence-journals.html#journal-plugin-api
[1]: https://doc.akka.io/docs/akka/current/persistence-journals.html#snapshot-store-plugin-api
[2]: https://doc.akka.io/docs/akka/current/persistence-query.html
[3]: https://r2dbc.io/
