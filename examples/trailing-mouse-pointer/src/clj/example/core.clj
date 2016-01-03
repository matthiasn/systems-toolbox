(ns example.core
  (:gen-class)
  (:require
    [matthiasn.systems-toolbox.switchboard :as sb]
    [matthiasn.systems-toolbox-sente.sente :as sente]
    [matthiasn.systems-toolbox.scheduler :as sched]
    [matthiasn.systems-toolbox-metrics.metrics :as metrics]
    [example.index :as index]
    [example.pointer :as ptr]
    [clojure.tools.logging :as log]
    [clj-pid.core :as pid]
    [io.aviso.logging :as pretty]))

(pretty/install-pretty-logging)
(pretty/install-uncaught-exception-handler)

(defonce switchboard (sb/component :server/switchboard))

(defn restart!
  "Starts (or restarts) a system built out of the specified subsystems. The switchboard will
  then fire up subsystems according to the blueprint maps, which are in passed in the second
  position of the :cmd/init-comp vectors. These subsystems are then wired to provide the
  communication paths required by the application.
  The system can be restarted on the REPL. For example, say we modify the example.pointer
  namespace by uncommenting the form beginning in line 19. Then, we can reload it:

      (require '[example.pointer :as ptr] :reload)

  Then, calling this function again will restart the system while maintaining the state of
  individual subsystems. In the UI, you can observe that the counter will have been retained,
  only the behavior will now be different, as randomly delayed messages will be introduced."
  []
  (sb/send-mult-cmd
    switchboard
    [;; First of all, we instantiate and wire a couple fo different components.
     [:cmd/init-comp (sente/cmp-map :server/ws-cmp index/index-page)] ; WebSocket component
     [:cmd/init-comp (sched/cmp-map :server/scheduler-cmp)] ; scheduling component
     [:cmd/init-comp (ptr/cmp-map :server/ptr-cmp)]         ; component for processing mouse moves
     [:cmd/init-comp (metrics/cmp-map :server/metrics-cmp)] ; component for metrics and stats
     ;; Then, messages of a given type are wired from one component to another.
     [:cmd/route-all {:from :server/ptr-cmp :to :server/ws-cmp}]
     [:cmd/route-all {:from :server/metrics-cmp :to :server/ws-cmp}]
     [:cmd/route {:from :server/ws-cmp :to :server/ptr-cmp}]
     [:cmd/route {:from :server/scheduler-cmp :to :server/ws-cmp :only :cmd/mouse-pos}]
     [:cmd/route {:from :server/scheduler-cmp :to :server/metrics-cmp}]
     [:cmd/route {:from :server/ptr-cmp :to :server/scheduler-cmp}]
     ;; Finally, schedule dispatch of :cmd/get-jvm-stats every 5 seconds.
     [:cmd/send {:to  :server/scheduler-cmp
                 :msg [:cmd/schedule-new
                       {:timeout 5000 :id :disp-stats :message [:cmd/get-jvm-stats] :repeat true}]}]]))

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
