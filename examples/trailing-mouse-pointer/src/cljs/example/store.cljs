(ns example.store
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]
            [cljs.core.match :refer-macros [match]]))

(defn mouse-pos-from-server!
  "Handler function for mouse position messages received from server."
  [app msg]
  (let [[_ pos] msg
        ws-client-meta (:client/ws-cmp (meta msg))
        rt-time (- (:out-timestamp ws-client-meta) (:in-timestamp ws-client-meta))
        ws-server-meta (:server/ws-cmp (meta msg))
        server-proc-time (- (:in-timestamp ws-server-meta) (:out-timestamp ws-server-meta))
        network-time (- rt-time server-proc-time)
        with-ts (assoc pos :rt-time rt-time)]
    (swap! app assoc :from-server with-ts)
    (swap! app update-in [:count] inc)
    (swap! app update-in [:rtt-times] conj rt-time)
    (swap! app update-in [:server-proc-times] conj server-proc-time)
    (swap! app update-in [:network-times] conj network-time)))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:cmd/mouse-pos         _] (mouse-pos-from-server! app msg)
         :else (prn "unknown msg in data-loop" msg)))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:count 0 :rtt-times [] :network-times [] :server-proc-times []})]
    app))

(defn component
  [cmp-id]
  (comp/make-component {:cmp-id   cmp-id
                        :state-fn mk-state
                        :handler  in-handler}))