(ns matthiasn.systems-toolbox.reagent
  (:require [reagent.core :as r :refer [create-class atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]))

(defn init
  "Return clean initial component state atom."
  [reagent-cmp-map dom-id init-state init-fn put-fn]
  (let [local (if init-state
                (atom init-state)
                (atom {}))
        observed (atom {})
        cmd (fn ([& r] (fn [e] (.stopPropagation e) (put-fn (vec r)))))
        reagent-cmp (create-class reagent-cmp-map)
        view-cmp-map {:observed observed
                      :local    local
                      :put-fn   put-fn
                      :cmd      cmd}]
    (r/render-component [reagent-cmp view-cmp-map] (by-id dom-id))
    (when init-fn (init-fn view-cmp-map))
    {:local local :observed observed}))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg-payload]}]
  (reset! (:observed cmp-state) msg-payload))

(defn component
  [{:keys [cmp-id view-fn lifecycle-callbacks dom-id initial-state init-fn cfg handler-map]}]
  (let [reagent-cmp-map (merge lifecycle-callbacks {:reagent-render view-fn})
        mk-state (partial init reagent-cmp-map dom-id initial-state init-fn)]
    (comp/make-component {:cmp-id            cmp-id
                          :state-fn          mk-state
                          :handler-map       handler-map
                          :state-pub-handler state-pub-handler
                          :opts              (merge cfg {:watch :local})})))
