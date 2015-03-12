(ns matthiasn.systems-toolbox.reagent
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [cljs.core.async :refer [chan pub sub buffer sliding-buffer pipe]]))

(defn by-id
  "Helper function, gets DOM element by ID."
  [id]
  (.getElementById js/document id))

(defn init
  "Return clean initial component state atom."
  [view-fn dom-id put-fn]
  (let [app (atom {})]
    (r/render-component [view-fn app put-fn] (by-id dom-id))
    app))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [app _ [_ state-snapshot]]
  (reset! app state-snapshot))

(defn component
  [view-fn dom-id]
  (let [make-state (partial init view-fn dom-id)]
    (comp/make-component make-state nil state-pub-handler)))
