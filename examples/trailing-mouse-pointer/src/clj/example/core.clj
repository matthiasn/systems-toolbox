(ns example.core
  (:gen-class)
  (:require
    [matthiasn.systems-toolbox.switchboard :as sb]
    [matthiasn.systems-toolbox.sente :as sente]
    [matthiasn.systems-toolbox.scheduler :as sched]
    [matthiasn.systems-toolbox.metrics :as metrics]
    [example.index :as index]
    [example.pointer :as ptr]
    [clojure.tools.namespace.repl :refer [refresh]]
    [clojure.tools.logging :as log]
    [clj-pid.core :as pid]))

(def switchboard (sb/component :server/switchboard))

(defn -main [& args]
  (pid/save "example.pid")
  (pid/delete-on-shutdown! "example.pid")

  (sb/send-mult-cmd
    switchboard
    [[:cmd/wire-comp (sente/component   :server/ws-cmp index/index-page 8010)] ; Component for WebSocket communication
     [:cmd/wire-comp (sched/component   :server/scheduler-cmp)]  ; Component for scheduling the dispatch of messages
     [:cmd/wire-comp (ptr/component     :server/ptr-cmp)]        ; Component for processing mouse moves
     [:cmd/wire-comp (metrics/component :server/metrics-cmp)]    ; Component for metrics and stats

     [:cmd/sub-comp-2   :server/ptr-cmp        :server/ws-cmp         :cmd/mouse-pos] ; from to type
     [:cmd/sub-comp     :server/scheduler-cmp  :server/ws-cmp         :cmd/mouse-pos] ; from to type
     [:cmd/sub-comp     :server/metrics-cmp    :server/ws-cmp         :stats/jvm]
     [:cmd/sub-comp     :server/scheduler-cmp  :server/metrics-cmp    :cmd/get-jvm-stats]
     [:cmd/sub-comp     :server/ptr-cmp        :server/scheduler-cmp  :cmd/schedule-new]

     [:cmd/send-to
      [:server/scheduler-cmp                    ; Schedule dispatch of :cmd/get-jvm-stats every 5 seconds.
       [:cmd/schedule-new {:timeout 5000 :id :disp-stats :message [:cmd/get-jvm-stats] :repeat true}]]]])

  (pid/save "example.pid")
  (log/info "Application started, PID" (pid/current)))