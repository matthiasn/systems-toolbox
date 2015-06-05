(ns example.store
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]
            [cljs.core.match :refer-macros [match]]))

(defn mouse-pos-from-server!
  "Handler function for mouse position messages received from server."
  [{:keys [cmp-state msg]}]
  (let [[_ pos] msg
        ws-client-meta (:client/ws-cmp (meta msg))
        rt-time (- (:out-timestamp ws-client-meta) (:in-timestamp ws-client-meta))
        ws-server-meta (:server/ws-cmp (meta msg))
        server-proc-time (- (:in-timestamp ws-server-meta) (:out-timestamp ws-server-meta))
        network-time (- rt-time server-proc-time)
        with-ts (assoc pos :rt-time rt-time)]
    (swap! cmp-state assoc :from-server with-ts)
    (swap! cmp-state update-in [:count] inc)
    (swap! cmp-state update-in [:rtt-times] conj rt-time)
    (swap! cmp-state update-in [:server-proc-times] conj server-proc-time)
    (swap! cmp-state update-in [:network-times] conj network-time)))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (atom {:count 0 :rtt-times [] :network-times [] :server-proc-times []}))

(defn component
  [cmp-id]
  (comp/make-component {:cmp-id   cmp-id
                        :state-fn mk-state
                        :handler-map {:cmd/mouse-pos mouse-pos-from-server!}}))
