(ns example.core
  (:require [example.spec]
            [example.store :as store]
            [example.ui-histograms :as hist]
            [example.ui-info :as info]
            [example.re-frame :as ui]
            [example.metrics :as metrics]
            [example.observer :as observer]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

(def sente-cfg {:relay-types      #{:mouse/pos
                                    :mouse/get-hist
                                    :firehose/cmp-put
                                    :firehose/cmp-recv
                                    :firehose/cmp-publish-state
                                    :firehose/cmp-recv-state}
                ;:msgs-on-firehose true
                })

; TODO: maybe firehose messages should implicitly be relayed?
(defn init! []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp
      #{(sente/cmp-map :client/ws-cmp sente-cfg)
        (ui/cmp-map :client/ui-cmp)
        (store/cmp-map :client/store-cmp)}]

     [:cmd/route {:from :client/ui-cmp
                  :to   #{:client/store-cmp :client/ws-cmp}}]

     [:cmd/route {:from #{:client/store-cmp
                          :client/ws-cmp}
                  :to   :client/store-cmp}]

     ;[:cmd/attach-to-firehose :client/ws-cmp]

     [:cmd/observe-state {:from :client/store-cmp
                          :to :client/ui-cmp}]])
  (metrics/init! switchboard)
  #_(observer/init! switchboard)
  )

(init!)
