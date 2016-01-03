(ns example.store)

(defn mouse-pos-from-server!
  "Handler function for mouse position messages received from server. Here, we first determine the
  round trip time (RTT) by subtracting the message creation timestamp from the timestamp when the
  message is finally received by the store component.
  Next, the server side processing time is determined. For this, we can use the timestamps from
  when the ws-cmp on the server side emits a message coming from the client and when the processed
  message is received back for delivery to the client.
  Finally, the component state is updated with the new measurements. Here, we're modifying the
  component state atom multiple times. One could also do this in a single transaction, should
  this be a performance issue. It doesn't seem that way but could still be interesting to figure out."
  [{:keys [cmp-state msg-payload msg-meta]}]
  (let [mouse-out-ts (:out-ts (:client/mouse-cmp msg-meta))
        store-in-ts (:in-ts (:client/store-cmp msg-meta))
        rt-time (- store-in-ts mouse-out-ts)
        srv-ws-meta (:server/ws-cmp msg-meta)
        srv-proc-time (- (:in-ts srv-ws-meta) (:out-ts srv-ws-meta))
        network-time (- rt-time srv-proc-time)
        with-rtt (assoc msg-payload :rt-time rt-time)]
    (swap! cmp-state assoc :from-server with-rtt)
    (swap! cmp-state update-in [:count] inc)
    (swap! cmp-state update-in [:rtt-times] conj rt-time)
    (swap! cmp-state update-in [:server-proc-times] conj srv-proc-time)
    (swap! cmp-state update-in [:network-times] conj network-time)))

(defn state-fn
  "Return clean initial component state atom."
  [put-fn]
  {:state (atom {:count 0
                 :rtt-times []
                 :network-times []
                 :server-proc-times []})})

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:cmd/mouse-pos mouse-pos-from-server!}})
