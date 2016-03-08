(ns example.store
  (:require [matthiasn.systems-toolbox.handler-utils :as hu]))

(defn inc-handler
  "Handler for incrementing specific counter"
  [{:keys [current-state msg-payload]}]
    {:new-state (update-in current-state [:counters msg-payload] inc)})

(defn dec-handler
  "Handler for decrementing specific counter"
  [{:keys [current-state msg-payload]}]
    {:new-state (update-in current-state [:counters msg-payload] dec)})

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

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:cnt/inc inc-handler
                 :cnt/dec dec-handler
                 :cnt/remove remove-handler
                 :cnt/add add-handler}})
