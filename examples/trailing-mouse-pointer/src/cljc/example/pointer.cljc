(ns example.pointer
  "This component receives messages, keeps a counter, decorates them with the state of the counter, and sends
  them back. Here, this provides a way to measure roundtrip time from the UI, as timestamps are recorded as
  the message flows through the system.")

(defn process-mouse-pos
  "Handler function for received mouse positions, increments counter and returns mouse position to sender."
  [{:keys [current-state msg-meta msg-payload]}]
  (let [new-state (update-in current-state [:count] inc)]
    {:new-state new-state
     :emit-msg (with-meta [:cmd/mouse-pos (assoc msg-payload :count (:count new-state))] msg-meta)}))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (fn [_] {:state (atom {:count 0})})
   :handler-map {:cmd/mouse-pos process-mouse-pos}})
