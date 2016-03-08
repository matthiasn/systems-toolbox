(ns example.core
  (:require [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.server :as sente]
            [example.index :as index]
            [clojure.tools.logging :as log]
            [clj-pid.core :as pid]
            [io.aviso.logging :as pretty]))

(pretty/install-pretty-logging)
(pretty/install-uncaught-exception-handler)

(defonce switchboard (sb/component :server/switchboard))

(defn restart!
  "Starts or restarts system by asking switchboard to fire up the provided ws-cmp, a scheduler
  component and the ptr component, which handles and counts messages about mouse moves."
  []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (sente/cmp-map :server/ws-cmp index/index-page)]]))

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
