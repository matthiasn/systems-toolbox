(ns example.ui-histograms
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [example.histogram :as h]
            [matthiasn.systems-toolbox.component :as st]
            [re-frame.core :refer [subscribe]]
            [example.hist-calc :as m]
            [reagent.core :as r]
            [example.utils :as u]))

(defn histograms-view
  "Renders histograms with different data sets, labels and colors."
  []
  (let [rtt-times (subscribe [:rtt-times])
        network-times (subscribe [:network-times])
        show-all? (r/atom false)]
    (fn histograms-render []
      (let [rtt-times (sort @rtt-times)]
        [:figure#histograms.fullwidth
         [:span.show
          {:on-click #(swap! show-all? not)}
          (if @show-all?
            "show single"
            "show all")]
         (if @show-all?
           [:div
            [:div
             [h/histogram-view
              rtt-times
              "Roundtrip t/ms" "#D94B61"]
             [h/histogram-view
              (m/percentile-range rtt-times 99)
              "Roundtrip t/ms (within 99th percentile)" "#D94B61"]
             [h/histogram-view
              (m/percentile-range rtt-times 95)
              "Roundtrip t/ms (within 95th percentile)" "#D94B61"]]
            #_
            (let [network-times (sort @network-times)]
              [:div
               [h/histogram-view
                network-times
                "Network t/ms" "#66A9A5"]
               [h/histogram-view
                (m/percentile-range network-times 99)
                "Network t/ms (within 99th percentile)" "#66A9A5"]
               [h/histogram-view
                (m/percentile-range network-times 95)
                "Network t/ms (within 95th percentile)" "#66A9A5"]])]
           [:div
            [:div
             [h/histogram-view
              rtt-times
              "Roundtrip t/ms" "#D94B61"]]])]))))
