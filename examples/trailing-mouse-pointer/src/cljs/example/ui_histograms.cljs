(ns example.ui-histograms
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.charts.histogram :as hist]))

(defn histogram-view
  "Renders an individual histogram for the given data, dimension, label and color."
  [data label color]
  [:svg {:width "100%" :viewBox "0 0 400 250"}
   (hist/histogram-view data 80 180 300 160 label color 0.8 25)])

(defn histograms-view
  "Renders histograms with different data sets, labels and colors."
  [{:keys [observed]}]
  (let [state @observed
        rtt-times (:rtt-times state)
        server-proc-times (:server-proc-times state)
        network-times (:network-times state)]
    [:div
     [:div
      [histogram-view rtt-times "Roundtrip t/ms" "#D94B61"]
      [histogram-view
       (hist/percentile-range rtt-times 99) "Roundtrip t/ms (within 99th percentile)" "#D94B61"]
      [histogram-view
       (hist/percentile-range rtt-times 95) "Roundtrip t/ms (within 95th percentile)" "#D94B61"]]
     [:div
      [histogram-view network-times "Network time t/ms (within 99th percentile)" "#66A9A5"]
      [histogram-view (hist/percentile-range network-times 95)
       "Network time t/ms (within 95th percentile)" "#66A9A5"]
      [histogram-view server-proc-times "Server processing time t/ms" "#F1684D"]]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn histograms-view
              :dom-id  "histograms"
              :cfg     {:throttle-ms           100
                        :msgs-on-firehose      true
                        :snapshots-on-firehose true}}))
