(ns matthiasn.systems-toolbox.reagent
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]))

(defn init
  "Return clean initial component state atom."
  [view-fn dom-id init-state put-fn]
  (let [app (if init-state
              (atom init-state)
              (atom {}))]
    (r/render-component [view-fn app put-fn] (by-id dom-id))
    app))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [app _ [_ state-snapshot]]
  (reset! app state-snapshot))

(defn component
  [cmp-id view-fn dom-id init-state]
  (let [make-state (partial init view-fn dom-id init-state)]
    (comp/make-component cmp-id make-state nil state-pub-handler)))
