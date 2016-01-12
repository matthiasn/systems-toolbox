(ns example.core-mock
  (:require [example.store :as store]
            [example.server-switchboard :as srvr-sb]
            [example.ui-histograms :as hist]
            [example.ui-mouse-moves :as mouse]
            [example.conf :as conf]
            [matthiasn.systems-toolbox-ui.charts.observer :as obs]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.mock :as sente-mock]
            [matthiasn.systems-toolbox-metrics.jvmstats :as jvmstats]
            [matthiasn.systems-toolbox.component :as comp]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

(defn init
  [client-ws-cmp]
  (sb/send-mult-cmd
    switchboard
    [client-ws-cmp                                          ; injected WebSocket communication component
     [:cmd/init-comp (hist/cmp-map :client/histogram-cmp)]  ; histograms component
     [:cmd/init-comp (mouse/cmp-map :client/mouse-cmp)]     ; UI component for capturing mouse moves
     [:cmd/init-comp (store/cmp-map :client/store-cmp)]     ; Data store component
     [:cmd/init-comp (jvmstats/cmp-map :client/jvmstats-cmp "jvm-stats-frame")] ;  UI component: JVM stats
     [:cmd/init-comp (obs/cmp-map :client/observer-cmp conf/observer-cfg-map)] ; UI component for observing system

     ;; Then, messages of a given type are wired from one component to another
     [:cmd/route-all {:from :client/mouse-cmp :to :client/ws-cmp}]
     [:cmd/route {:from :client/ws-cmp :to :client/store-cmp}]
     [:cmd/route {:from :client/ws-cmp :to :client/jvmstats-cmp}]
     [:cmd/observe-state {:from :client/store-cmp :to :client/histogram-cmp}]
     [:cmd/observe-state {:from :client/store-cmp :to :client/mouse-cmp}]

     ;; Finally, wire firehose with all messages into the observer component
     [:cmd/attach-to-firehose :client/observer-cmp]]))

(let [client-ws-cmp (comp/make-component (sente-mock/cmp-map :client/ws-cmp {:sente-uid :client}))
      server-ws-cmp (comp/make-component (sente-mock/cmp-map :server/ws-cmp {}))]
  (init [:cmd/wire-comp client-ws-cmp])
  (srvr-sb/restart! [:cmd/wire-comp server-ws-cmp])
  (sente-mock/connect-sente-mocks client-ws-cmp server-ws-cmp))
