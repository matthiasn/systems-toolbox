(ns example.pointer
  "This component receives messages, keeps a counter, decorates them with the state of the counter, and sends
  them back. Here, this provides a way to measure roundtrip time from the UI, as timestamps are recorded as
  the message flows through the system.
  Also records a recent history of mouse positions for all clients, which the component provides to clients
  upon request.")

(defn process-mouse-pos
  "Handler function for received mouse positions, increments counter and returns mouse position to sender."
  [{:keys [current-state msg-meta msg-payload]}]
  (let [new-state (-> current-state
                      (update-in [:count] inc)
                      (update-in [:mouse-moves] #(vec (take-last 1000 (conj % msg-payload)))))]
    {:new-state new-state
     :emit-msg (with-meta [:mouse/pos (assoc msg-payload :count (:count new-state))] msg-meta)}))

(defn get-mouse-hist
  "Gets the recent mouse position history from server."
  [{:keys [current-state msg-meta]}]
  {:emit-msg (with-meta [:mouse/hist (:mouse-moves current-state)] msg-meta)})

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (fn [_] {:state (atom {:count 0 :mouse-moves []})})
   :handler-map {:mouse/pos      process-mouse-pos
                 :mouse/get-hist get-mouse-hist}})
