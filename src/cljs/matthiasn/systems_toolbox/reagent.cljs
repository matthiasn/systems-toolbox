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
    (r/render-component [view-fn observed local put-fn cmd] (by-id dom-id))
    {:local local :observed observed}))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [app _ [_ state-snapshot]]
  (reset! (:observed app) state-snapshot))

(defn component
  ([cmp-id view-fn dom-id init-state] (component cmp-id view-fn dom-id init-state nil))
  ([cmp-id view-fn dom-id init-state cfg]
   (let [make-state (partial init view-fn dom-id init-state)]
     (comp/make-component cmp-id make-state nil state-pub-handler (merge cfg {:watch :local})))))
