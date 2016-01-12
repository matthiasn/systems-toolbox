(ns example.core
  (:require
    [matthiasn.systems-toolbox.switchboard :as sb]
    [matthiasn.systems-toolbox-sente.server :as sente]
    [matthiasn.systems-toolbox-metrics.metrics :as metrics]
    [example.index :as index]
    [example.server-switchboard :as srvr-sb]
    [clojure.tools.logging :as log]
    [clj-pid.core :as pid]
    [io.aviso.logging :as pretty]))

(pretty/install-pretty-logging)
(pretty/install-uncaught-exception-handler)

(defn add-metrics!
  "Adds a metrics component to the provided switchboard, and wires it up as needed."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (metrics/cmp-map :server/metrics-cmp)]
     [:cmd/route-all {:from :server/metrics-cmp :to :server/ws-cmp}]
     [:cmd/route {:from :server/scheduler-cmp :to :server/metrics-cmp}]
     [:cmd/send {:to  :server/scheduler-cmp
                 :msg [:cmd/schedule-new
                       {:timeout 5000 :id :disp-stats :message [:cmd/get-jvm-stats] :repeat true}]}]]))

(def ws-cmp (sente/cmp-map :server/ws-cmp index/index-page))

(defn -main
  "Starts the application from command line, saves and logs process ID. The system that is fired up when
  restart! is called proceeds in core.async's thread pool. Since we don't want the application to exit when
  just because the current thread is out of work, we just put it to sleep."
  [& args]
  (pid/save "example.pid")
  (pid/delete-on-shutdown! "example.pid")
  (log/info "Application started, PID" (pid/current))
  (srvr-sb/restart! [:cmd/init-comp ws-cmp])
  (add-metrics! srvr-sb/switchboard)
  (Thread/sleep Long/MAX_VALUE))
