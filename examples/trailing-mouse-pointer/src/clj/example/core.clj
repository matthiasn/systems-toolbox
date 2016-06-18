(ns example.core
  (:require [example.spec]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.server :as sente]
            [matthiasn.systems-toolbox-metrics.metrics :as metrics]
            [example.index :as index]
            [example.server-switchboard :as srvr-sb]
            [clojure.tools.logging :as log]
            [clj-pid.core :as pid]
            [example.pointer :as ptr]
            [matthiasn.systems-toolbox.scheduler :as sched]))

(defonce switchboard (sb/component :server/switchboard))

(defn restart!
  "Starts or restarts system by asking switchboard to fire up the provided ws-cmp, a scheduler
  component and the ptr component, which handles and counts messages about mouse moves."
  []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (sente/cmp-map :server/ws-cmp index/sente-map)]  ; WebSocket component
     [:cmd/init-comp (sched/cmp-map :server/scheduler-cmp)]      ; scheduling component
     [:cmd/init-comp (ptr/cmp-map :server/ptr-cmp)]              ; component for processing mouse moves
     [:cmd/init-comp (metrics/cmp-map :server/metrics-cmp)]      ; metrics component
     [:cmd/route-all {:from #{:server/ptr-cmp :server/metrics-cmp}
                      :to   :server/ws-cmp}] ; route all messages to ws-cmp
     [:cmd/route {:from :server/ws-cmp :to :server/ptr-cmp}] ;
     [:cmd/route {:from :server/scheduler-cmp :to :server/metrics-cmp}]
     [:cmd/send {:to  :server/scheduler-cmp
                 :msg [:cmd/schedule-new
                       {:timeout 5000 :id :disp-stats :message [:cmd/get-jvm-stats] :repeat true}]}]
     ]))  ; route handled messages to ptr-cmp

(defn -main
  "Starts the application from command line, saves and logs process ID. The system that is fired up when
  restart! is called proceeds in core.async's thread pool. Since we don't want the application to exit when
  just because the current thread is out of work, we just put it to sleep."
  [& args]
  (pid/save "example.pid")
  (pid/delete-on-shutdown! "example.pid")
  (log/info "Application started, PID" (pid/current))
  (restart!)
  (Thread/sleep Long/MAX_VALUE))
