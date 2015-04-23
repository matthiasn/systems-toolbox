(ns example.core
  (:require [example.ui-mouse-svg :as ui-svg]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.sente :as sente]
            [matthiasn.systems-toolbox.ui.jvmstats :as jvmstats]
            [matthiasn.systems-toolbox.ui.observe-state :as obs-st]
            [matthiasn.systems-toolbox.ui.observe-messages :as obs-msg]))

(enable-console-print!)

(def switchboard (sb/component))

(sb/send-mult-cmd
  switchboard
  [[:cmd/wire-comp (sente/component  :ws-cmp)]       ; WebSocket communication component
   [:cmd/wire-comp (ui-svg/component  :svg-cmp)]     ; UI component: mouse moves inside SVG
   [:cmd/wire-comp (jvmstats/component :jvmstats-cmp "jvm-stats-frame")]  ;  UI component: JVM stats

   [:cmd/wire-comp (obs-st/component   :observe-state-cmp "snapshots")]  ; UI component: observing snapshots
   [:cmd/wire-comp (obs-msg/component  :observe-msgs-cmp "messages")]  ; UI component: observing messages

   [:cmd/tap-comp
    [:svg-cmp     ;    »───»───»──╢  Routes all incoming messages from url-cmp to :ws-cmp (and implicitly to server).
     :ws-cmp]]    ; <= «═══«═══«══╝

   [:cmd/tap-comp
    [[:ws-cmp             ;    »───»───»──╢
      :svg-cmp]           ;    »───»───»──╢  Route messages to observation component.
     :observe-msgs-cmp]]  ; <= «═══«═══«══╝

   [:cmd/sub-comp
    [[:ws-cmp :stats/jvm]]  ;    »─[:stats/jvm]─»─╢   Subscribe UI component for JVM stats to messages on WebSockets.
    :jvmstats-cmp]          ; <= «═══«═══«═══«═══«╝

   [:cmd/sub-comp
    [[:ws-cmp :cmd/mouse-pos-proc]] ;  »─[:cmd/mouse-pos-proc]─»─╢
    :svg-cmp]                       ;    <= «═══«═══«═══«═══«═══«╝

   [:cmd/sub-comp-state
    [:svg-cmp                 ; => »═══»═══»══╗
     [[:observe-state-cmp]]]] ;    «───«───«──╢

   ])
