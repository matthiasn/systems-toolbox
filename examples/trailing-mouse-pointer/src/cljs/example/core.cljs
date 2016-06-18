(ns example.core
  (:require [example.spec]
            [example.store :as store]
            [example.ui-histograms :as hist]
            [example.ui-mouse-moves :as mouse]
            [example.conf :as conf]
            [matthiasn.systems-toolbox-ui.charts.observer :as obs]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [matthiasn.systems-toolbox-metrics.jvmstats :as jvmstats]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

(defn init
  [client-ws-cmp]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp client-ws-cmp]                         ; injected WebSocket communication component
     [:cmd/init-comp (hist/cmp-map :client/histogram-cmp)]  ; histograms component
     [:cmd/init-comp (mouse/cmp-map :client/mouse-cmp)]     ; UI component for capturing mouse moves
     [:cmd/init-comp (store/cmp-map :client/store-cmp)]     ; Data store component
     [:cmd/init-comp (jvmstats/cmp-map :client/jvmstats-cmp "jvm-stats-frame")] ;  UI component: JVM stats
     [:cmd/init-comp (obs/cmp-map :client/observer-cmp conf/observer-cfg-map)] ; UI component for observing system

     ;; Then, messages of a given type are wired from one component to another
     [:cmd/route {:from :client/mouse-cmp :to :client/ws-cmp}]
     [:cmd/route {:from :client/ws-cmp :to #{:client/store-cmp :client/jvmstats-cmp}}]
     [:cmd/observe-state {:from :client/store-cmp :to #{:client/mouse-cmp :client/histogram-cmp}}]

     ;; Finally, wire firehose with all messages into the observer component
     [:cmd/attach-to-firehose :client/observer-cmp]]))

(init (sente/cmp-map :client/ws-cmp {:relay-types #{:cmd/mouse-pos}}))
