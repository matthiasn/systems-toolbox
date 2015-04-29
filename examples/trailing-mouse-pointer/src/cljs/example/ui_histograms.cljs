(ns example.ui-histograms
  (:require [matthiasn.systems-toolbox.reagent :as r]
            [matthiasn.systems-toolbox.ui.histogram :as hist]))

(defn histogram-view
  "Renders histograms with roundtrip times."
  [app put-fn]
  (let [state @app
        rtt-times (:rtt-times state)
        server-proc-times (:server-proc-times state)
        network-times (:network-times state)]
    [:div.pure-g
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :viewBox "0 0 400 250"}
       (hist/histogram-view rtt-times 80 180 300 160 "Roundtrip t/ms" "#D94B61")]]
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :viewBox "0 0 400 250"}
       (hist/histogram-view (hist/percentile-range rtt-times 99) 80 180 300 160
        "Roundtrip t/ms (within 99th percentile)" "#D94B61")]]
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :viewBox "0 0 400 250"}
       (hist/histogram-view (hist/percentile-range rtt-times 95) 80 180 300 160
        "Roundtrip t/ms (within 95th percentile)" "#D94B61")]]
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :viewBox "0 0 400 250"}
       (hist/histogram-view network-times 80 180 300 160 "Network time t/ms" "#66A9A5")]]
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :viewBox "0 0 400 250"}
       (hist/histogram-view (hist/percentile-range network-times 99) 80 180 300 160
        "Network time t/ms (within 99th percentile)" "#66A9A5")]]
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :viewBox "0 0 400 250"}
       (hist/histogram-view (hist/percentile-range network-times 95) 80 180 300 160
        "Network time t/ms (within 95th percentile)" "#66A9A5")]]
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :viewBox "0 0 400 250"}
       (hist/histogram-view server-proc-times 80 180 300 160 "Server processing time t/ms" "#F1684D")]]
     ]))

(defn component [cmp-id] (r/component cmp-id histogram-view "histograms" {}))
