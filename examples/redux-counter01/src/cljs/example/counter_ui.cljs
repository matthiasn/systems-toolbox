(ns example.counter-ui
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]))

(defn counter-view
  "Renders counter view which observes the state held by the state component. Clicking
  on it sends an increment command message."
  [idx v put-fn]
  [:div
   [:h1 v]
   [:button {:on-click #(put-fn [:cnt/dec idx])} "dec"]
   [:button {:on-click #(put-fn [:cnt/inc idx])} "inc"]])

(defn counters-view
  [{:keys [current-state put-fn]}]
  (let [indexed (map-indexed vector (:counters current-state))]
    [:div
     [:button {:on-click #(put-fn [:cnt/remove])} "remove"]
     [:button {:on-click #(put-fn [:cnt/add])} "add"]
     (for [[idx v] indexed]
            ^{:key idx} [counter-view idx v put-fn])]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn counters-view
              :dom-id  "counter"}))
