# Akka Persistence R2DBC

An implementation of the Akka [Journal][0] and [Snapshot store][1] persistence
plugin APIs, and the full [Persistence Query][2] API with [R2DBC][3]. The plugin
has a few additional extensions documented below.

## Extensions

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
