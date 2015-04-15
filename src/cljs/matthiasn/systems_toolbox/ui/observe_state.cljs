(ns matthiasn.systems-toolbox.ui.observe-state
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as r :refer [atom]]
            [cljs.core.match :refer-macros [match]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id now]]))

(defn snapshot-view
  "Render application state snapshot."
  [app]
  (let [state @app
        show (:show state)]
    [:div
     [:a {:style {:float :right :margin "10px" :color (if show "steelblue" "#EEE")}
          :on-click #(swap! app update-in [:show] not)} "snapshots"]
     (when show
       (for [[timestamp from snapshot] (reverse (take-last 10 (:snapshots state)))]
         ^{:key (str "snapshot-" timestamp)}
         [:div
          [:h3 (str from " - Snapshot")]
          [:h6 timestamp]
          [:pre [:code (str snapshot)]]]))]))

(defn mk-state
  "Return clean initial component state atom."
  [dom-id]
  (fn [put-fn]
    (let [app (atom {:snapshots [] :show false})]
      (r/render-component [snapshot-view app] (by-id dom-id))
      app)))

(defn recv-snapshot
  "Handle receiving a snapshot."
  [app from snapshot]
  (swap! app assoc :snapshots (conj (:snapshots @app) [(now) from snapshot])))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:app-state snapshot] (recv-snapshot app (:from (meta msg)) snapshot)
         :else (println "Unmatched event:" msg)))

(defn component
  [cmp-id dom-id]
  (comp/make-component cmp-id (mk-state dom-id) nil state-pub-handler))
