# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).


## [0.6.1-alpha5] - 2016-08-20
### Changed
- improved error handling in msg-handler-loop

## [0.6.1-alpha4] - 2016-08-01
### Changed
- Formatting
- Firehose message handling improved

## [0.6.1-alpha3] - 2016-08-01
### Changed
- Firehose message ID added

## [0.6.1-alpha2] - 2016-07-12
### Changed
-  Clojure 1.9.0-alpha10

## [0.6.1-alpha1] - 2016-07-06
### BREAKING CHANGES
- Clojure 1.9 required
- component IDs MUST be namespaced keywords
- message types MUST be namespaced keywords

## [0.5.22] - 2016-06-06
### Changed
- support multiple messages in `send-to-self` and `emit-msg`. `emit-msgs` deprecated

## [0.5.20] - 2016-06-04
### Changed
- dependencies; no dependency on specific Clojure version

## [0.5.19] - 2016-05-28
### Changed
- components support `observed-xform` function: applied to observed snapshot before resetting the local observed state

## [0.5.18] - 2016-05-11
### Changed
- `:emit-msgs` and `:send-to-self` in return map of handler

## [0.5.15] - 2016-03-30
### Changed
- Clojure 1.8, ClojureScript 1.8.40, dependencies

## [0.5.14] - 2016-03-08
### Changed
- handler functions can now be free from side effects

## [0.5.13] - 2016-03-05
### Changed
- additional tests; checking for overhead introduced by library

## [0.5.12] - 2016-03-04
### Changed
- library now testable on JVM and browser

## [0.5.11] - 2016-03-03
### Changed
- performance improvements in browser by using requestAnimationFrame more sparingly

## [0.5.10] - 2016-02-21
### Changed
- some refactoring and additional test

## [0.5.9] - 2016-02-19
### Changed
- broader use of blocking put; additional test

## [0.5.8] - 2016-02-17
### Changed
- send-msg blocks by default; component tests

## [0.5.7] - 2016-01-29
### Changed
- PR from clyfe: stoppable scheduler

## [0.5.1] - 2016-01-03
### Changed
- Sente and Reagent/UI components moved into separate repos

## [0.4.11] - 2015-12-30
### Changed
- Kafka consumer and producer moved from systems-toolbox into separate repo

## [0.4.10] - 2015-12-29
### Changed
- allow specifying a user-id-fn for sente (from PR)

## [0.4.9] - 2015-12-27
### Changed
- Kafka producer and consumer components

## [0.4.8] - 2015-12-22
### Changed
- fwd-as-w-meta function from PR

## [0.4.7] - 2015-12-22
### Changed
- allow user-specified state-pub-handler in views, for example for resetting state on logout

## [0.4.6] - 2015-12-21
### Changed
- fn for sending message to single component

## [0.4.5] - 2015-12-17
### Changed
- alternative sente config; version bumps

## [0.4.2] - 2015-12-07
### Changed
- minor histogram improvements

## [0.4.1] - 2015-12-05
### BREAKING CHANGES
- state-fn now needs to return a map, where the fresh component state is expected under the `:state` key. In addition, an optional `:shutdown-fn` can be specified, which will be called when the component is shutdown or restarted. This is for example useful when resources such as web servers or database connections need to be shut down.


## [0.3.15] - 2015-12-03
### Changed
- documentation; dependency upgrades; Clojure 1.8.0-RC3 in sample

## [0.3.14] - 2015-11-30
### Changed
- histogram more configurable

## [0.3.12] - 2015-11-26
### Changed
- more reasonable x-axis increments

## [0.3.11] - 2015-11-09
### Changed
- latest version of ClojureScript (much faster compiles)

## [0.3.10] - 2015-10-31
### Changed
- simplified component wiring

## [0.3.9] - 2015-10-30
### Changed
- allow passing of middleware to the sente component, besides the index-page-fn

## [0.3.8] - 2015-10-28
### Changed
- version bumps, including core.async v0.2.371

## [0.3.7] - 2015-10-26
### Changed
- fix for exception when putting too many messages at once

## [0.3.6] - 2015-10-15
### Changed
- optional message filtering via predicate function (see example)

## [0.3.1] - 2015-10-13
### Changed
- component initialization can now also be handled by the switchboard. See commit message and sample.


## [0.2.31] - 2015-10-12
### Changed
- unhandled handler: this function is called for each message that is not handled by another handler in the :handler-map of a component.

## [0.2.30] - 2015-09-23
### Changed
- version bumps

## [0.2.29] - 2015-08-29
### Changed
- Buffer WS messages until the connection is opened.

## [0.2.28] - 2015-08-28
### Changed
- host and port configuration via environment variables (e.g. for use with Docker)

## [0.2.27] - 2015-08-21
### Changed
- dependency updates & web server options

## [0.2.26] - 2015-08-10
### Changed
- Immutant-web instead of http-kit: as far as open source projects go, http-kit does not look very healthy.
- Less logging: the log component is probably not very useful at all. There's no good reason not to use 'conventional' logging inside handler code. Being able to inspect input and output messages without recompile should greatly reduce the need for logging anyway.

## [0.2.25] - 2015-08-08
### Changed
- refactoring, more readable component namespace

## [0.2.24] - 2015-08-06
### Changed
- recording the sequence of handling components
- dependency bumps

## [0.2.23] - 2015-08-06
### Changed
- UUIDs sent as strings

## [0.2.22] - 2015-08-05
### Changed
- Only non-firehose messages are wrapped when putting on firehose channel

## [0.2.21] - 2015-08-04
### Changed
- messages that are sent between component started and system completely wired are kept

## [0.2.20] - 2015-07-30
### Changed
- Expose metadata explicitly in firehose messages

## [0.2.19] - 2015-07-30
### Changed
- Reagent components now support :lifecycle-callbacks parameter that can be used to attach React components' lifecycle methods such as :component-will-update.

## [0.2.18] - 2015-07-28
### Changed
- When a message is first emitted, a :tag UUID is attached to the metadata, which allows tracking a message on its way through the system. Also, a correlation UUID is attached, which uniquely marks an emitted message.
- The full sequence of components that a message passes through is recorded on the metadata.

## [0.2.17] - 2015-07-23
### Changed
- View components can call init-fn on initialization. This can for example be useful when attaching a watcher to the local state atom.

## [0.2.16] - 2015-07-21
### Changed
- no requirement for scheduler id, default is the keyword in the first position inside message to be sent
- switchboard prints component state for inspection when receiving [cmd/print-cmp-state cmp-id] message
- simplification of and documentation for `route-handler`

## [0.2.15] - 2015-07-09
### Changed
- minor rewrites after 'lein kibit'

## [0.2.14] - 2015-07-09
### Changed
- Documentation

## [0.2.13] - 2015-07-08
### Changed
- Reloadable components without modification, e.g. for use with Figwheel
- Specify components that can't be reloaded, e.g. WebSockets connection component

## [0.2.10] - 2015-07-06
### Changed
- Better error handling and logging in message handler loops
- Using aviso/pretty for exception logging in example

## [0.2.9] - 2015-07-05
### Changed
- Figwheel in example
- Publish state snapshot on reload

## [0.2.8] - 2015-07-03
### Changed
- Reader conditionals instead of the now-deprecated CLJX

## [0.2.7] - 2015-07-01
### Changed
- Clojure 1.7 final instead of release candidate
- observer component tweaks

## [0.2.6] - 2015-06-24
### Changed
- Custom state snapshot transformer function used in switchboard. With that, routing snapshots to server no longer fails. Minimal functionality, state snapshots from switchboard should include more information, not just component keys.
- Observer component feeds entirely off of messages on firehose; no need for subscribing to switchboard state.s

## [0.2.5] - 2015-06-23
### Changed
- Custom state snapshot function: allows stripping state of functions, channels and the like. Particularly useful in switchboard where state snapshots could otherwise not traverse the WebSockets connection between client and server.
- Should the full state with non-serializable values such as channels ever be needed, there could still be a message for retrieving the full state map.

## [0.2.4] - 2015-06-19
### Changed
- Enable handler maps inside UI components. This allows for UI components that are more independent and don't require an external store/state component. This feature can be useful for components that do not share any state with other components.


[0.6.1-alpha4]: https://github.com/matthiasn/systems-toolbox/compare/v0.6.1-alpha3...v0.6.1-alpha4
[0.6.1-alpha3]: https://github.com/matthiasn/systems-toolbox/compare/v0.6.1-alpha2...v0.6.1-alpha3
[0.6.1-alpha2]: https://github.com/matthiasn/systems-toolbox/compare/v0.6.1-alpha1q...v0.6.1-alpha2
[0.6.1-alpha1]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.22...v0.6.1-alpha1
[0.5.22]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.20...v0.5.22
[0.5.20]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.19...v0.5.20
[0.5.19]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.18...v0.5.19
[0.5.18]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.15...v0.5.18
[0.5.15]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.14...v0.5.15
[0.5.14]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.13...v0.5.14
[0.5.13]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.12...v0.5.13
[0.5.12]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.11...v0.5.12
[0.5.11]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.10...v0.5.11
[0.5.10]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.9...v0.5.10
[0.5.9]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.8...v0.5.9
[0.5.8]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.7...v0.5.8
[0.5.7]: https://github.com/matthiasn/systems-toolbox/compare/v0.5.1...v0.5.7
[0.5.1]: https://github.com/matthiasn/systems-toolbox/compare/v0.4.10...v0.5.1
[0.4.10]: https://github.com/matthiasn/systems-toolbox/compare/v0.4.9...v0.4.10
[0.4.9]: https://github.com/matthiasn/systems-toolbox/compare/v0.4.8...v0.4.9
[0.4.8]: https://github.com/matthiasn/systems-toolbox/compare/v0.4.7...v0.4.8
[0.4.7]: https://github.com/matthiasn/systems-toolbox/compare/v0.4.6...v0.4.7
[0.4.6]: https://github.com/matthiasn/systems-toolbox/compare/v0.4.5...v0.4.6
[0.4.5]: https://github.com/matthiasn/systems-toolbox/compare/v0.4.2...v0.4.5
[0.4.2]: https://github.com/matthiasn/systems-toolbox/compare/v0.4.1...v0.4.2
[0.4.1]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.15...v0.4.1
[0.3.15]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.14...v0.3.15
[0.3.14]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.12...v0.3.14
[0.3.12]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.11...v0.3.12
[0.3.11]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.10...v0.3.11
[0.3.10]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.9...v0.3.10
[0.3.9]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.8...v0.3.9
[0.3.8]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.7...v0.3.8
[0.3.7]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.6...v0.3.7
[0.3.6]: https://github.com/matthiasn/systems-toolbox/compare/v0.3.1...v0.3.6
[0.3.1]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.33...v0.3.1
[0.2.33]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.32...v0.2.33
[0.2.32]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.31...v0.2.32
[0.2.31]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.30...v0.2.31
[0.2.30]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.29...v0.2.30
[0.2.29]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.28...v0.2.29
[0.2.28]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.27...v0.2.28
[0.2.27]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.26...v0.2.27
[0.2.26]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.25...v0.2.26
[0.2.25]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.24...v0.2.25
[0.2.24]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.23...v0.2.24
[0.2.23]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.22...v0.2.23
[0.2.22]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.21...v0.2.22
[0.2.21]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.20...v0.2.21
[0.2.20]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.19...v0.2.20
[0.2.19]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.18...v0.2.19
[0.2.18]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.17...v0.2.18
[0.2.17]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.16...v0.2.17
[0.2.16]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.15...v0.2.16
[0.2.15]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.14...v0.2.15
[0.2.14]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.13...v0.2.14
[0.2.13]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.12...v0.2.13
[0.2.12]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.11...v0.2.12
[0.2.11]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.10...v0.2.11
[0.2.10]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.9...v0.2.10
[0.2.9]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.8...v0.2.9
[0.2.8]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.7...v0.2.8
[0.2.7]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.6...v0.2.7
[0.2.6]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.5...v0.2.6
[0.2.5]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.4...v0.2.5
[0.2.4]: https://github.com/matthiasn/systems-toolbox/compare/v0.2.3...v0.2.4
