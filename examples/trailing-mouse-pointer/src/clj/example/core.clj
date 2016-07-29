(ns example.core
  (:require [example.spec]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.server :as sente]
            [example.metrics :as metrics]
            [matthiasn.systems-toolbox-observer.probe :as probe]
            [example.index :as index]
            [clojure.tools.logging :as log]
            [clj-pid.core :as pid]
            [matthiasn.systemd-watchdog.core :as wd]
            [example.pointer :as ptr])
  (:gen-class))

(defonce switchboard (sb/component :server/switchboard))

(defn restart!
  "Starts or restarts system by asking switchboard to fire up the provided
   ws-cmp and the ptr component, which handles and counts messages about mouse
   moves."
  []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(sente/cmp-map :server/ws-cmp index/sente-map)
                       (ptr/cmp-map :server/ptr-cmp)}]
     [:cmd/route {:from :server/ptr-cmp :to :server/ws-cmp}]
     [:cmd/route {:from :server/ws-cmp :to :server/ptr-cmp}]])
  (metrics/start! switchboard)
  #_
  (probe/start! switchboard))

(defn -main
  "Starts the application from command line, saves and logs process ID. The
   system that is fired up when restart! is called proceeds in core.async's
   thread pool. Since we don't want the application to exit when just because
   the current thread is out of work, we just put it to sleep."
  [& args]
  (pid/save "example.pid")
  (pid/delete-on-shutdown! "ws-example.pid")
  (log/info "Application started, PID" (pid/current))
  (restart!)
  (wd/start-watchdog! 5000)
  (Thread/sleep Long/MAX_VALUE))
