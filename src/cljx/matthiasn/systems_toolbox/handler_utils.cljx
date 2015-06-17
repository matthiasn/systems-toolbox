(ns matthiasn.systems-toolbox.handler-utils
  #+clj (:gen-class))

(defn fwd-as
  "Creates a handler function that sends the payload of handled message as a new message type while discarding
  any metadata on the original message."
  [new-type]
  (fn
    [{:keys [put-fn msg-payload]}]
    (put-fn [new-type msg-payload])))

(defn run-handler
  "Runs another handler function with a new message and otherwise the same context."
  ([handler-key msg-payload msg-map]
   (let [handler-fn (handler-key (:handler-map msg-map))]
     (when handler-fn (handler-fn (assoc msg-map :msg-type handler-key
                                                 :msg-payload msg-payload
                                                 :msg [handler-key msg-payload])))))
  ([handler-key msg-map]
   (let [handler-fn (handler-key (:handler-map msg-map))]
     (when handler-fn (handler-fn (assoc msg-map :msg-type handler-key
                                                 :msg [handler-key]))))))