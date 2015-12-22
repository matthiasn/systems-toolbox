(ns matthiasn.systems-toolbox.handler-utils
  #?(:clj
     (:gen-class))
  (:require [clojure.set :refer [subset?]]))

(defn fwd-as
  "Creates a handler function that sends the payload of handled message as a new message type while discarding
  any metadata on the original message."
  [new-type]
  (fn
    [{:keys [put-fn msg-payload]}]
    (put-fn [new-type msg-payload])))

(defn fwd-as-w-meta
  "Creates a handler function that sends the payload of the handled message as a new message type preserving
  metadata of the original message."
  [new-type]
  (fn
    [{:keys [put-fn msg-payload msg-meta]}]
    (put-fn (with-meta [new-type msg-payload] msg-meta))))

(defn run-handler
  "Runs another handler function with a new message and otherwise the same context."
  ([handler-key msg-payload msg-map]
   (let [handler-fn (handler-key (:handler-map msg-map))]
     (when handler-fn (handler-fn (assoc msg-map :msg-type handler-key
                                                 :msg-payload msg-payload
                                                 :msg [handler-key msg-payload])))))
  ([handler-key msg-map]
   ;; A common mistake is to call (run-handler) with a handler-key and msg-payload, but not with
   ;; a msg-map. In such case, this fn would do nothing and fail silently, leaving user in a blank.
   ;; Assert to verify against that.
   (assert (subset? #{:cmp-state :msg-meta :msg-payload} (into #{} (keys msg-map)))
           "(run-handler) invoked with invalid arguments. Make sure you did pass msg-map.")
   (let [handler-fn (handler-key (:handler-map msg-map))]
     (when handler-fn (handler-fn (assoc msg-map :msg-type handler-key
                                                 :msg [handler-key]))))))
