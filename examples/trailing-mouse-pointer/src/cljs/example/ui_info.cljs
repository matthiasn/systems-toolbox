(ns example.ui-info
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.helpers :refer [by-id]]))

(defn info-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [{:keys [observed put-fn]}]
  (let [state-snapshot @observed
        last-rt (:rt-time (:from-server state-snapshot))
        rtt-times (:rtt-times state-snapshot)
        mx (apply max rtt-times)
        mn (apply min rtt-times)
        mean (.round js/Math (if (seq rtt-times) (/ (apply + rtt-times) (count rtt-times)) 0))
        local-pos (:local state-snapshot)
        latency-string (str mean " mean / " mn " min / " mx " max / " last-rt " last" )]
    [:div
     [:strong "Mouse Moves Processed: "] (:count state-snapshot) [:br]
     [:strong "Processed since Startup: "] (:count (:from-server state-snapshot)) [:br]
     [:strong "Current position: "] "x: " (:x local-pos) " y: " (:y local-pos) [:br]
     [:strong "Latency (ms): "] latency-string [:br]
     [:button {:on-click #(put-fn [:cmd/show-all])} "show all"]]))

(defn cmp-map
  "Configuration map for systems-toolbox-ui component."
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn info-view
              :dom-id  "info"
              :cfg     {:msgs-on-firehose      true
                        :snapshots-on-firehose true}}))
