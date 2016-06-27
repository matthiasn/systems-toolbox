(ns example.ui-histograms
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.charts.histogram :as h]))

(defn histograms-view
  "Renders histograms with different data sets, labels and colors."
  [{:keys [observed]}]
  (let [state @observed
        rtt-times (:rtt-times state)
        server-proc-times (:server-proc-times state)
        network-times (:network-times state)]
    [:div
     [:div
      [h/histogram-view rtt-times "Roundtrip t/ms" "#D94B61"]
      [h/histogram-view
       (h/percentile-range rtt-times 99) "Roundtrip t/ms (within 99th percentile)" "#D94B61"]
      [h/histogram-view
       (h/percentile-range rtt-times 95) "Roundtrip t/ms (within 95th percentile)" "#D94B61"]]
     [:div
      [h/histogram-view network-times "Network time t/ms (within 99th percentile)" "#66A9A5"]
      [h/histogram-view (h/percentile-range network-times 95)
       "Network time t/ms (within 95th percentile)" "#66A9A5"]
      [h/histogram-view server-proc-times "Server processing time t/ms" "#F1684D"]]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn histograms-view
              :dom-id  "histograms"
              :cfg     {:throttle-ms           100
                        :msgs-on-firehose      true
                        :snapshots-on-firehose true}}))
