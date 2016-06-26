(ns example.store)

(defn mouse-pos-handler
  "Handler function for mouse position messages. When message from server:
    - determine the round trip time (RTT) by subtracting the message creation timestamp
      from the timestamp when the message is finally received by the store component.
    - determine server side processing time is determined. For this, we can use the timestamps
      from when the ws-cmp on the server side emits a message coming from the client and when the
      processed message is received back for delivery to the client.
    - update component state with the new mouse location under :from-server.
   When message received locally, only update position in :local."
  [{:keys [current-state msg-payload msg-meta]}]
  (let [new-state
        (if (:count msg-payload)
          (let [mouse-out-ts (:out-ts (:client/mouse-cmp msg-meta))
                store-in-ts (:in-ts (:client/store-cmp msg-meta))
                rt-time (- store-in-ts mouse-out-ts)
                srv-ws-meta (:server/ws-cmp msg-meta)
                srv-proc-time (- (:in-ts srv-ws-meta) (:out-ts srv-ws-meta))]
            (-> current-state
                (assoc-in [:from-server] (assoc msg-payload :rt-time rt-time))
                (update-in [:count] inc)
                (update-in [:rtt-times] conj rt-time)
                (update-in [:server-proc-times] conj srv-proc-time)
                (update-in [:network-times] conj (- rt-time srv-proc-time))))
          (-> current-state
              (assoc-in [:local] msg-payload)
              (update-in [:local-hist] conj msg-payload)))]
    {:new-state new-state}))

(defn show-all-handler
  "Toggles boolean value in component state for provided key."
  [{:keys [current-state msg-payload]}]
  {:new-state (update-in current-state [:show-all msg-payload] not)})

(defn mouse-hist-handler
  "Saves the received vector with mouse positions in component state."
  [{:keys [current-state msg-payload]}]
  {:new-state (assoc-in current-state [:server-hist] msg-payload)})

(defn state-fn
  "Return clean initial component state atom."
  [_put-fn]
  {:state (atom {:count             0
                 :rtt-times         []
                 :network-times     []
                 :server-proc-times []
                 :local             {:x 0 :y 0}
                 :show-all          {:local  false
                                     :remote false}})})

(defn cmp-map
  "Configuration map that specifies how to instantiate component."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:mouse/pos    mouse-pos-handler
                 :cmd/show-all show-all-handler
                 :mouse/hist   mouse-hist-handler}
   :opts        {:msgs-on-firehose      true
                 :snapshots-on-firehose true}})
