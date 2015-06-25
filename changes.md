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