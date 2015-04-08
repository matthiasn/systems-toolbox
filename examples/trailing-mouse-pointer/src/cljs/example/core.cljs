(ns example.core
  (:require [example.ui-mouse-svg :as ui-svg]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.sente :as sente]
            [matthiasn.systems-toolbox.ui.jvmstats :as jvmstats]))

(def switchboard (sb/component))

(sb/send-mult-cmd
  switchboard
  [[:cmd/wire-comp (sente/component  :ws-cmp)]       ; WebSocket communication component
   [:cmd/wire-comp (ui-svg/component  :svg-cmp)]     ; UI component: mouse moves inside SVG
   [:cmd/wire-comp (jvmstats/component :jvmstats-cmp "jvm-stats-frame")]  ;  UI component: JVM stats

   #_[:cmd/tap-comp
    [:ws-cmp         ;    »───»───»──╢  Routes all incoming WebSockets messages to the implicitly
     :log-cmp]]      ; <= «═══«═══«══╝  instantiated logging component. Only used for development purposes.

   [:cmd/tap-comp
    [:svg-cmp     ;    »───»───»──╢  Routes all incoming messages from url-cmp to :ws-cmp (and implicitly to server).
     :ws-cmp]]    ; <= «═══«═══«══╝

   [:cmd/sub-comp
    [[:ws-cmp :stats/jvm]]  ;    »─[:stats/jvm]─»─╢   Subscribe UI component for JVM stats to messages on WebSockets.
    :jvmstats-cmp]          ; <= «═══«═══«═══«═══«╝

   [:cmd/sub-comp
    [[:ws-cmp :cmd/mouse-pos-proc]] ;  »─[:cmd/mouse-pos-proc]─»─╢
    :svg-cmp]])                     ;    <= «═══«═══«═══«═══«═══«╝

