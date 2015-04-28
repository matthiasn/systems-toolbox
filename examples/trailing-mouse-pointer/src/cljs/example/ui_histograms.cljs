(ns example.ui-histograms
  (:require [matthiasn.systems-toolbox.reagent :as r]
            [matthiasn.systems-toolbox.ui.histogram :as hist]))

(defn histogram-view
  "Renders histograms with roundtrip times."
  [app put-fn]
  (let [state @app
        rtt-times (:rtt-times state)]
    [:div.pure-g {:style {:background-color :white}}
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :style {:background-color :white} :viewBox "0 0 400 250"}
       [hist/histogram-view rtt-times 80 180 300 160 "Roundtrip t/ms"]]]
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :style {:background-color :white} :viewBox "0 0 400 250"}
       [hist/histogram-view (hist/percentile-range rtt-times 99) 80 180 300 160
        "Roundtrip t/ms (within 99th percentile)"]]]
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :style {:background-color :white} :viewBox "0 0 400 250"}
       [hist/histogram-view (hist/percentile-range rtt-times 95) 80 180 300 160
        "Roundtrip t/ms (within 95th percentile)"]]]]))

(defn component [cmp-id] (r/component cmp-id histogram-view "histograms" {}))
