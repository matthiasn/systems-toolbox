## v0.2.19

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