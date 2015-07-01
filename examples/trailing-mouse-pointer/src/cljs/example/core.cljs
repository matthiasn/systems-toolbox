(ns example.core
  (:require [example.store :as store]
            [example.ui-histograms :as hist]
            [example.ui-mouse-moves :as mouse]
            [matthiasn.systems-toolbox.ui.observer :as obs]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.sente :as sente]
            [matthiasn.systems-toolbox.ui.jvmstats :as jvmstats]))

(enable-console-print!)

(def switchboard (sb/component :client/switchboard))

(defn init []
  (sb/send-mult-cmd
    switchboard                                             ;; Below, we're interacting with the switchboard component we just created above.
    [;; First of all, we instantiate and wire a couple fo different components.
     [:cmd/wire-comp (sente/component :client/ws-cmp)]      ; WebSocket communication component
     [:cmd/wire-comp (hist/component :client/histogram-cmp)] ; UI component for histograms
     [:cmd/wire-comp (mouse/component :client/mouse-cmp)]   ; UI component for capturing mouse moves
     [:cmd/wire-comp (store/component :client/store-cmp)]   ; Data store component
     [:cmd/wire-comp (jvmstats/component :client/jvmstats-cmp "jvm-stats-frame")] ;  UI component: JVM stats

     [:cmd/wire-comp (obs/component :client/observer-cmp "observer"
                                    {:link-distance 280
                                     :charge        -5000
                                     :width         1000
                                     :height        1000
                                     :fixed-nodes   {:client/switchboard {:x 500 :y 200}
                                                     :client/ws-cmp      {:x 200 :y 200}
                                                     :client/store-cmp   {:x 500 :y 500}}})]

     ;; Then, messages of a given type are wired from one component to another.
     ;[:cmd/route {:from :client/mouse-cmp :to :client/ws-cmp :only :cmd/mouse-pos}]
     [:cmd/route-all {:from :client/mouse-cmp :to :client/ws-cmp}]
     [:cmd/route {:from :client/ws-cmp :to :client/store-cmp}]
     [:cmd/route {:from :client/ws-cmp :to :client/jvmstats-cmp}]
     [:cmd/observe-state {:from :client/store-cmp :to :client/histogram-cmp}]
     [:cmd/observe-state {:from :client/store-cmp :to :client/mouse-cmp}]
     [:cmd/observe-state {:from :client/switchboard :to :client/observer-cmp}]

     ;; Finally, wire firehose with all messages into the observer component.
     [:cmd/attach-to-firehose :client/observer-cmp]])
  )
(init)
