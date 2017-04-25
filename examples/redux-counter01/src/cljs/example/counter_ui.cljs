(ns example.counter-ui
  (:require [reagent.core :as r]
            [re-frame.core :refer [reg-sub subscribe]]
            [re-frame.db :as rdb]
            [cljs.pprint :as pp]))

(reg-sub :state (fn [db _] db))

(defn pp-div [current-state]
  [:pre [:code (with-out-str (pp/pprint current-state))]])

(defn counter-view
  "Renders individual counter view, with buttons for increasing or decreasing
   the value."
  [idx v put-fn]
  [:div
   [:h1 v]
   [:button {:on-click #(put-fn [:cnt/dec {:counter idx}])} "dec"]
   [:button {:on-click #(put-fn [:cnt/inc {:counter idx}])} "inc"]])

(defn counters-view
  "Renders counters view which observes the state held by the state component.
  Contains two buttons for adding or removing counters, plus a counter-view
  for every element in the observed state."
  [put-fn]
  (let [state (subscribe [:state])]
    (fn counters-view-render [put-fn]
      (let [current-state @state
            indexed (map-indexed vector (:counters current-state))]
        [:div.counters
         [pp-div current-state]
         [:button {:on-click #(put-fn [:cnt/remove])} "remove"]
         [:button {:on-click #(put-fn [:cnt/add])} "add"]
         (for [[idx v] indexed]
           ^{:key idx} [counter-view idx v put-fn])]))))

(defn state-fn
  "Renders main view component and wires the central re-frame app-db as the
   observed component state, which will then be updated whenever the store-cmp
   changes."
  [put-fn]
  (r/render [counters-view put-fn] (.getElementById js/document "counter"))
  {:observed rdb/app-db})

(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})
