(ns example.core
  (:require [example.spec]
            [example.store :as store]
            [example.ui-histograms :as hist]
            [example.ui-mouse-moves :as mouse]
            [example.ui-info :as info]
            [example.conf :as conf]
            [matthiasn.systems-toolbox-ui.charts.observer :as obs]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [matthiasn.systems-toolbox-metrics.jvmstats :as jvmstats]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

(defn init! []
  (sb/send-mult-cmd
    switchboard
    [;; First, instantiate components
     [:cmd/init-comp
      #{(sente/cmp-map :client/ws-cmp {:relay-types #{:cmd/mouse-pos} :msgs-on-firehose true})
        (mouse/cmp-map :client/mouse-cmp)
        (info/cmp-map  :client/info-cmp)
        (store/cmp-map :client/store-cmp)
        (hist/cmp-map  :client/histogram-cmp)
        (jvmstats/cmp-map :client/jvmstats-cmp {:dom-id "jvm-stats-frame" :msgs-on-firehose true})
        (obs/cmp-map   :client/observer-cmp conf/observer-cfg-map)}]
     ;; Then, messages of a given type are wired from one component to another
     [:cmd/route {:from :client/mouse-cmp :to #{:client/store-cmp :client/ws-cmp}}]
     [:cmd/route {:from :client/ws-cmp :to #{:client/store-cmp :client/jvmstats-cmp}}]
     [:cmd/route {:from :client/info-cmp :to :client/store-cmp}]
     [:cmd/observe-state {:from :client/store-cmp :to #{:client/mouse-cmp
                                                        :client/histogram-cmp
                                                        :client/info-cmp}}]

     ;; Finally, wire firehose with all messages into the observer component
     [:cmd/attach-to-firehose :client/observer-cmp]]))

(init!)
