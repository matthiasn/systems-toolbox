(ns example.pointer
  "This component receives messages, keeps a counter, decorates them with the state of the counter, and sends
  them back. Here, this provides a way to measure roundtrip time from the UI, as timestamps are recorded as
  the message flows through the system.")

(defn ptr-state-fn
  "Creates fresh component state with a counter.."
  [_]
  {:state (atom {:count 0})})

(defn process-mouse-pos
  "Handler function for received mouse positions, increments counter and returns mouse position to sender."
  [{:keys [cmp-state msg-meta msg-payload put-fn]}]
  (swap! cmp-state update-in [:count] inc)
  (put-fn (with-meta [:cmd/mouse-pos (assoc msg-payload :count (:count @cmp-state))] msg-meta)))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    ptr-state-fn
   :handler-map {:cmd/mouse-pos process-mouse-pos}})
