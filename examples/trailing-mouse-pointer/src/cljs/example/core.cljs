(ns example.core
  (:require [example.spec]
            [example.store :as store]
            [example.ui-histograms :as hist]
            [example.ui-mouse-moves :as mouse]
            [example.ui-info :as info]
            [example.metrics :as metrics]
            [example.observer :as observer]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

; TODO: maybe firehose messages should implicitly be relayed?
(defn init! []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp
      #{(sente/cmp-map :client/ws-cmp {:relay-types      #{:mouse/pos
                                                           :mouse/get-hist
                                                           :firehose/cmp-put
                                                           :firehose/cmp-recv
                                                           :firehose/cmp-publish-state
                                                           :firehose/cmp-recv-state}
                                       :msgs-on-firehose true})
        (mouse/cmp-map :client/mouse-cmp)
        (info/cmp-map :client/info-cmp)
        (store/cmp-map :client/store-cmp)
        (hist/cmp-map :client/histogram-cmp)}]
     [:cmd/route {:from :client/mouse-cmp
                  :to   #{:client/store-cmp :client/ws-cmp}}]
     [:cmd/route {:from :client/ws-cmp
                  :to   :client/store-cmp}]
     [:cmd/route {:from :client/info-cmp
                  :to   #{:client/store-cmp :client/ws-cmp}}]
     [:cmd/observe-state {:from :client/store-cmp
                          :to   #{:client/mouse-cmp :client/histogram-cmp :client/info-cmp}}]])
  (metrics/init! switchboard)
  (observer/init! switchboard))

(init!)
