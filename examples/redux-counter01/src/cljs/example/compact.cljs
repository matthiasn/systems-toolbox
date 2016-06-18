(ns example.compact
  "This namespace is an attempt at making the example as terse as possible."
  (:require [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.helpers :as h]))

(defn upd-handler [f]
  (fn [{:keys [current-state msg-payload]}]
    {:new-state (update-in current-state [:counters msg-payload] f)}))

(defn add-remove-handler [f]
  (fn [{:keys [current-state]}]
    {:new-state (update-in current-state [:counters] f)}))

(sb/send-mult-cmd (sb/component :client/switchboard)
  [[:cmd/init-comp (r/cmp-map {:cmp-id  :client/cnt-cmp
                               :view-fn (fn [{:keys [current-state put-fn]}]
                                          (let [indexed (map-indexed vector (:counters current-state))]
                                            [:div.counters [h/pp-div current-state]
                                             [:button {:on-click #(put-fn [:cnt/remove])} "remove"]
                                             [:button {:on-click #(put-fn [:cnt/add])} "add"]
                                             (for [[idx v] indexed]
                                               ^{:key idx} [:div [:h1 v]
                                                            [:button {:on-click #(put-fn [:cnt/dec idx])} "dec"]
                                                            [:button {:on-click #(put-fn [:cnt/inc idx])} "inc"]])]))
                               :dom-id  "counter"})]
   [:cmd/init-comp {:cmp-id      :client/store-cmp
                    :state-fn    (fn [_put-fn] {:state (atom {:counters [2 0 1]})})
                    :handler-map {:cnt/inc    (upd-handler inc)
                                  :cnt/dec    (upd-handler dec)
                                  :cnt/remove (add-remove-handler #(into [] (butlast %)))
                                  :cnt/add    (add-remove-handler #(conj % 0))}}]
   [:cmd/route {:from :client/cnt-cmp :to :client/store-cmp}]
   [:cmd/observe-state {:from :client/store-cmp :to :client/cnt-cmp}]])
