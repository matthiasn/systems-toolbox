(ns matthiasn.systems-toolbox.reagent
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]))

(defn init
  "Return clean initial component state atom."
  [view-fn dom-id init-state put-fn]
  (let [local (if init-state
                (atom init-state)
                (atom {}))
        observed (atom {})
        cmd (fn ([& r] (fn [e] (.stopPropagation e) (put-fn (into [] r)))))]
    (r/render-component [view-fn {:observed observed
                                  :local    local
                                  :put-fn   put-fn
                                  :cmd      cmd}]
                        (by-id dom-id))
    {:local local :observed observed}))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg-payload]}]
  (reset! (:observed cmp-state) msg-payload))

(defn component
  [{:keys [cmp-id view-fn dom-id initial-state cfg handler-map]}]
  (let [mk-state (partial init view-fn dom-id initial-state)]
    (comp/make-component {:cmp-id            cmp-id
                          :state-fn          mk-state
                          :handler-map       handler-map
                          :state-pub-handler state-pub-handler
                          :opts              (merge cfg {:watch :local})})))
