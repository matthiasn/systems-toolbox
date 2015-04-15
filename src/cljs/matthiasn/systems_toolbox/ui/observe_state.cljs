(ns matthiasn.systems-toolbox.ui.observe-state
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as r :refer [atom]]
            [cljs.core.match :refer-macros [match]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]))

(defn now [] (.toISOString (js/Date.)))

(defn snapshot-view
  "Render application state snapshot."
  [app]
  (let [state @app
        [timestamp last-snapshot] (last (:snapshots state))
        show (:show state)]
    [:div
     [:a {:style {:float :right :margin "10px" :color (if show "steelblue" "#EEE")}
          :on-click #(swap! app update-in [:show] not)} "snapshots"]
     (when show
       [:div
        [:h3 (:heading state)]
        [:h6 timestamp]
        [:pre [:code (str last-snapshot)]]])]))

(defn mk-state
  "Return clean initial component state atom."
  [dom-id heading]
  (fn [put-fn]
    (let [app (atom {:snapshots [] :show false})]
      (swap! app assoc :heading heading)
      (r/render-component [snapshot-view app] (by-id dom-id))
      app)))

(defn recv-snapshot
  "Handle receiving a snapshot."
  [app snapshot]
  (swap! app assoc :snapshots (conj (:snapshots @app) [(now) snapshot])))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:app-state snapshot] (recv-snapshot app snapshot)
         :else (println "Unmatched event:" msg)))

(defn component
  [cmp-id dom-id heading]
  (comp/make-component cmp-id (mk-state dom-id heading) nil state-pub-handler))
