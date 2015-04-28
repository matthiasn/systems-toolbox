(ns example.store
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]
            [cljs.core.match :refer-macros [match]]))

(defn mouse-pos-from-server!
  "Handler function for mouse position messages received from server."
  [app msg]
  (let [ws-meta (:client-ws-cmp (meta msg))
        [_ pos] msg
        rt-time (- (:recv-timestamp ws-meta) (:sent-timestamp ws-meta))
        with-ts (assoc pos :rt-time rt-time)]
    (swap! app assoc :from-server with-ts)
    (swap! app update-in [:count] inc)
    (swap! app update-in [:rtt-times] conj rt-time)))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:cmd/mouse-pos         _] (mouse-pos-from-server! app msg)
         [:cmd/mouse-pos-local pos] (swap! app assoc :pos pos)
         :else (prn "unknown msg in data-loop" msg)))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:count 0 :rtt-times []})]
    app))

(defn component
  [cmp-id]
  (comp/make-component cmp-id mk-state in-handler nil))