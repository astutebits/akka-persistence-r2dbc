# Akka Persistence R2DBC
[![CircleCI](https://circleci.com/gh/astutebits/akka-persistence-r2dbc.svg?style=svg)](https://circleci.com/gh/astutebits/akka-persistence-r2dbc)

An implementation of the Akka [Journal][0] and [Snapshot store][1] persistence
plugin APIs, and the full [Persistence Query][2] API with [R2DBC][3]. The plugin
has a few additional extensions documented below.

## Configuration

Unlike the other `AsyncWriteJournal` calls, the `asyncReplayMessages` one is not
protected by a circuit-breaker, for a good reason:

> This call is NOT protected with a circuit-breaker because it may take long
> time to replay all events. The plugin implementation itself must protect
> against an unresponsive backend store and make sure that the returned
> Future is completed with success or failure within reasonable time. It is
> not allowed to ignore completing the future.

The plugin guarantees that the Future is completed by imposing a time limit on
the SQL stream's completion. You can configure the timeout with the
`replay-messages-timeout` parameter, the default value is `10s` (in line with
the circuit breaker settings).

## Extensions

* `akka.serialization.AsyncSerializer` support
* `Atomic Projections` gives you the ability to persist an event and run
  projections for a "local" read-side in an atomic transaction.

### Atomic Projections

The goal of the extension is to allow you to enjoy the benefits of CQRS and
Event Sourcing without incurring the costs of running a more complex
infrastructure for those cases when you really don't _have_ to.

## Contributing

Contributions via GitHub pull requests are gladly accepted from their original
author. Along with any pull requests, please state that the contribution is your
original work and that you license the work to the project under the project’s
open source license. Whether you state this explicitly, by submitting any
copyrighted material via pull request, email, or other means you agree to
license the material under the project’s open source license and warrant that
you have the legal authority to do so.

## License

This project is licensed under the Apache 2.0 license.

[0]: https://doc.akka.io/docs/akka/current/persistence-journals.html#journal-plugin-api
[1]: https://doc.akka.io/docs/akka/current/persistence-journals.html#snapshot-store-plugin-api
[2]: https://doc.akka.io/docs/akka/current/persistence-query.html
[3]: https://r2dbc.io/
