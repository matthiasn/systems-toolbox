(ns example.core
  (:require [example.ui-mouse-svg :as ui-svg]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.sente :as sente]
            [matthiasn.systems-toolbox.ui.jvmstats :as jvmstats]))

(enable-console-print!)

(def switchboard (sb/component))

(sb/send-mult-cmd
  switchboard
  [[:cmd/wire-comp (sente/component    :client/ws-cmp)]      ; WebSocket communication component
   [:cmd/wire-comp (ui-svg/component   :client/svg-cmp)]     ; UI component: mouse moves inside SVG
   [:cmd/wire-comp (jvmstats/component :client/jvmstats-cmp "jvm-stats-frame")]  ;  UI component: JVM stats

   [:cmd/sub-comp-2   :client/ws-cmp   :client/svg-cmp        :cmd/mouse-pos] ; from to type
   [:cmd/sub-comp     :client/ws-cmp   :client/jvmstats-cmp   :stats/jvm]])