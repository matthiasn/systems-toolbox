(ns example.store
  "In this namespace, the app state is managed. One can only interact with the state by sending
  immutable messages. Each such message is then handled by a handler function. These handler functions
  here are pure functions, they receive message and previous state and return the new state.

  Both the messages passed around and the new state returned by the handlers are validated using
  clojure.spec. This eliminates an entire class of possible bugs, where failing to comply with
  data structure expectations might now immediately become obvious."
  (:require [cljs.spec :as s]))

(defn inc-handler
  "Handler for incrementing specific counter"
  [{:keys [current-state msg-payload]}]
  {:new-state (update-in current-state [:counters (:counter msg-payload)] #(+ % 1))})

(defn dec-handler
  "Handler for decrementing specific counter"
  [{:keys [current-state msg-payload]}]
  {:new-state (update-in current-state [:counters (:counter msg-payload)] dec)})

(defn remove-handler
  "Handler for removing last counter"
  [{:keys [current-state]}]
  {:new-state (update-in current-state [:counters] #(into [] (butlast %)))})

(defn add-handler
  "Handler for adding counter at the end"
  [{:keys [current-state]}]
  {:new-state (update-in current-state [:counters] conj 0)})

(defn state-fn
  "Returns clean initial component state atom"
  [_put-fn]
  {:state (atom {:counters [2 0 1]})})

;; validate messages using clojure.spec
(s/def :redux-ex1/counter #(and (integer? %) (>= % 0)))
(s/def :cnt/inc (s/keys :req-un [:redux-ex1/counter]))
(s/def :cnt/dec (s/keys :req-un [:redux-ex1/counter]))

;; validate component state using clojure.spec
(s/def :redux-ex1/counters (s/coll-of integer? []))
(s/def :redux-ex1/store-spec (s/keys :req-un [:redux-ex1/counters]))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :state-spec  :redux-ex1/store-spec
   :handler-map {:cnt/inc    inc-handler
                 :cnt/dec    dec-handler
                 :cnt/remove remove-handler
                 :cnt/add    add-handler}})
