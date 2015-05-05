(ns example.core
  (:require [example.store :as store]
            [example.ui-histograms :as hist]
            [example.force :as force]
            [example.ui-mouse-moves :as mouse]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.sente :as sente]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [matthiasn.systems-toolbox.ui.jvmstats :as jvmstats]))

(enable-console-print!)

(def switchboard (sb/component :client/switchboard))

(sb/send-mult-cmd
  switchboard
  [[:cmd/wire-comp (sente/component    :client/ws-cmp)]         ; WebSocket communication component
   [:cmd/wire-comp (hist/component     :client/histogram-cmp)]  ; UI component for histograms
   [:cmd/wire-comp (force/component    :client/force-cmp)]      ; UI component for force layout
   [:cmd/wire-comp (mouse/component    :client/mouse-cmp)]      ; UI component for capturing mouse moves
   [:cmd/wire-comp (sched/component    :client/scheduler-cmp)]  ; Component for scheduling the dispatch of messages
   [:cmd/wire-comp (store/component    :client/store-cmp)]      ; Data store component
   [:cmd/wire-comp (jvmstats/component :client/jvmstats-cmp "jvm-stats-frame")]  ;  UI component: JVM stats

   [:cmd/sub-comp  :client/mouse-cmp  :client/ws-cmp         :cmd/mouse-pos] ; from to type
   [:cmd/sub-comp  :client/mouse-cmp  :client/store-cmp      :cmd/mouse-pos-local] ; from to type
   [:cmd/sub-comp  :client/ws-cmp     :client/store-cmp      :cmd/mouse-pos] ; from to type
   [:cmd/sub-comp  :client/store-cmp  :client/histogram-cmp  :app-state]
   [:cmd/sub-comp  :client/store-cmp  :client/mouse-cmp      :app-state]
   [:cmd/sub-comp  :client/ws-cmp     :client/jvmstats-cmp   :stats/jvm]

   [:cmd/tap-sw-firehose :client/force-cmp]
   [:cmd/sub-comp :client/scheduler-cmp :client/force-cmp :cmd/trigger-update]

   [:cmd/send-to [:client/scheduler-cmp
     [:cmd/schedule-new {:timeout 100 :id :ui-updates :message [:cmd/trigger-update] :repeat true}]]]

   ])