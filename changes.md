## v0.4.9 - December 27th, 2015

```clojure
[matthiasn/systems-toolbox "0.4.9"]
```

* Kafka producer and consumer components


## v0.4.8 - December 22nd, 2015

```clojure
[matthiasn/systems-toolbox "0.4.8"]
```

* fwd-as-w-meta function from PR


## v0.4.7 - December 22nd, 2015

```clojure
[matthiasn/systems-toolbox "0.4.7"]
```

* allow user-specified state-pub-handler in views, for example for resetting state on logout


## v0.4.6 - December 21st, 2015

```clojure
[matthiasn/systems-toolbox "0.4.6"]
```

* fn for sending message to single component


## v0.4.5 - December 17th, 2015

```clojure
[matthiasn/systems-toolbox "0.4.5"]
```

* alternative sente config; version bumps


## v0.4.2 - December 7th, 2015

```clojure
[matthiasn/systems-toolbox "0.4.2"]
```

* minor histogram improvements


## v0.4.1 - December 5th, 2015

```clojure
[matthiasn/systems-toolbox "0.4.1"]
```

* BREAKING: state-fn now needs to return a map, where the fresh component state is expected under the `:state` key. In addition, an optional `:shutdown-fn` can be specified, which will be called when the component is shutdown or restarted. This is for example useful when resources such as web servers or database connections need to be shut down.


## v0.3.15 - December 3rd, 2015

```clojure
[matthiasn/systems-toolbox "0.3.15"]
```

* documentation; dependency upgrades; Clojure 1.8.0-RC3 in sample


## v0.3.14 - November 30th, 2015

```clojure
[matthiasn/systems-toolbox "0.3.14"]
```

* histogram more configurable


## v0.3.12 - November 26th, 2015

```clojure
[matthiasn/systems-toolbox "0.3.12"]
```

* more reasonable x-axis increments


## v0.3.11 - November 9th, 2015

```clojure
[matthiasn/systems-toolbox "0.3.11"]
```

* latest version of ClojureScript (much faster compiles)


## v0.3.10 - October 31st, 2015

```clojure
[matthiasn/systems-toolbox "0.3.10"]
```

* simplified component wiring


## v0.3.9 - October 30th, 2015

```clojure
[matthiasn/systems-toolbox "0.3.9"]
```

* allow passing of middleware to the sente component, besides the index-page-fn


## v0.3.8 - October 28th, 2015

```clojure
[matthiasn/systems-toolbox "0.3.8"]
```

* version bumps, including core.async v0.2.371


## v0.3.7 - October 26th, 2015

```clojure
[matthiasn/systems-toolbox "0.3.7"]
```

* fix for exception when putting too many messages at once


## v0.3.6 - October 15th, 2015

```clojure
[matthiasn/systems-toolbox "0.3.6"]
```

* optional message filtering via predicate function (see example)


## v0.3.1 - October 13th, 2015

```clojure
[matthiasn/systems-toolbox "0.3.1"]
```

* component initialization can now also be handled by the switchboard. See commit message and sample.


## v0.2.31 - October 12th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.31"]
```

* unhandled handler: this function is called for each message that is not handled by another handler in the :handler-map of a component.


## v0.2.30 - September 23rd, 2015

```clojure
[matthiasn/systems-toolbox "0.2.30"]

* version bumps


## v0.2.29 - August 29th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.29"]

* Buffer WS messages until the connection is opened.


## v0.2.28 - August 28th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.28"]

* host and port configuration via environment variables (e.g. for use with Docker)


## v0.2.27 - August 21st, 2015

```clojure
[matthiasn/systems-toolbox "0.2.27"]

* dependency updates & web server options


## v0.2.26 - August 10th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.26"]
```

* Immutant-web instead of http-kit: as far as open source projects go, http-kit does not look very healthy.
* Less logging: the log component is probably not very useful at all. There's no good reason not to use 'conventional' logging inside handler code. Being able to inspect input and output messages without recompile should greatly reduce the need for logging anyway.


## v0.2.25 - August 8th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.25"]
```

* refactoring, more readable component namespace


## v0.2.24 - August 6th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.24"]
```

* recording the sequence of handling components
* dependency bumps


## v0.2.23 - August 6th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.23"]
```

* UUIDs sent as strings


## v0.2.22 - August 5th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.22"]
```

* Only non-firehose messages are wrapped when putting on firehose channel


## v0.2.21 - August 4th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.21"]
```

* messages that are sent between component started and system completely wired are kept


## v0.2.20 - July 30th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.20"]
```

* Expose metadata explicitly in firehose messages


## v0.2.19 - July 30th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.19"]
```

* Reagent components now support :lifecycle-callbacks parameter that can be used to attach React components' lifecycle methods such as :component-will-update.

## v0.2.18 - July 28th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.18"]
```

* When a message is first emitted, a :tag UUID is attached to the metadata, which allows tracking a message on its way through the system. Also, a correlation UUID is attached, which uniquely marks an emitted message. 
* The full sequence of components that a message passes through is recorded on the metadata.


## v0.2.17 - July 23rd, 2015

```clojure
[matthiasn/systems-toolbox "0.2.17"]
```

* View components can call init-fn on initialization. This can for example be useful when attaching a watcher to the local state atom.


## v0.2.16 - July 21st, 2015

```clojure
[matthiasn/systems-toolbox "0.2.16"]
```

* no requirement for scheduler id, default is the keyword in the first position inside message to be sent
* switchboard prints component state for inspection when receiving [cmd/print-cmp-state cmp-id] message
* simplification of and documentation for `route-handler`


## v0.2.15 - July 9th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.15"]
```

* minor rewrites after 'lein kibit'


## v0.2.14 - July 9th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.14"]
```

* Documentation


## v0.2.13 - July 8th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.13"]
```

* Reloadable components without modification, e.g. for use with Figwheel
* Specify components that can't be reloaded, e.g. WebSockets connection component


## v0.2.10 - July 6th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.10"]
```

* Better error handling and logging in message handler loops
* Using aviso/pretty for exception logging in example


## v0.2.9 - July 5th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.9"]
```

* Figwheel in example
* Publish state snapshot on reload


## v0.2.8 - July 3rd, 2015

```clojure
[matthiasn/systems-toolbox "0.2.8"]
```

* Reader conditionals instead of the now-deprecated CLJX


## v0.2.7 - July 1st, 2015

```clojure
[matthiasn/systems-toolbox "0.2.7"]
```

* Clojure 1.7 final instead of release candidate
* observer component tweaks


## v0.2.6 - June 24th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.6"]
```

* Custom state snapshot transformer function used in switchboard. With that, routing snapshots to server no longer fails. Minimal functionality, state snapshots from switchboard should include more information, not just component keys.
* Observer component feeds entirely off of messages on firehose; no need for subscribing to switchboard state.s


## v0.2.5 - June 23th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.5"]
```

* Custom state snapshot function: allows stripping state of functions, channels and the like. Particularly useful in switchboard where state snapshots could otherwise not traverse the WebSockets connection between client and server.
* Should the full state with non-serializable values such as channels ever be needed, there could still be a message for retrieving the full state map.

## v0.2.4 - June 19th, 2015

```clojure
[matthiasn/systems-toolbox "0.2.4"]
```

* Enable handler maps inside UI components. This allows for UI components that are more independent and don't require an external store/state component. This feature can be useful for components that do not share any state with other components.