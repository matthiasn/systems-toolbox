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
    [clj-pid.core :as pid]
    [io.aviso.logging :as pretty]))

(pretty/install-pretty-logging)
(pretty/install-uncaught-exception-handler)

(def switchboard (sb/component :server/switchboard))

(defn -main [& args]
  (pid/save "example.pid")
  (pid/delete-on-shutdown! "example.pid")

  (sb/send-mult-cmd
    switchboard  ;; Below, we're interacting with the switchboard component we just created above.
    [;; First of all, we instantiate and wire a couple fo different components.
     [:cmd/wire-comp (sente/component   :server/ws-cmp index/index-page "0.0.0.0" 8010)]
     [:cmd/wire-comp (sched/component   :server/scheduler-cmp)]  ; Component for scheduling the dispatch of messages
     [:cmd/wire-comp (ptr/component     :server/ptr-cmp)]        ; Component for processing mouse moves
     [:cmd/wire-comp (metrics/component :server/metrics-cmp)]    ; Component for metrics and stats

     ;; Then, messages of a given type are wired from one component to another.
     [:cmd/route-all {:from :server/ptr-cmp :to :server/ws-cmp}]
     [:cmd/route-all {:from :server/ptr-cmp :to :server/log-cmp}]

     [:cmd/route-all {:from :server/metrics-cmp :to :server/ws-cmp}]
     [:cmd/route {:from :server/ws-cmp :to :server/ptr-cmp}]
     [:cmd/route {:from :server/scheduler-cmp :to :server/ws-cmp :only :cmd/mouse-pos}]
     [:cmd/route {:from :server/scheduler-cmp :to :server/metrics-cmp}]
     [:cmd/route {:from :server/ptr-cmp :to :server/scheduler-cmp}]

     ;; Finally, schedule dispatch of :cmd/get-jvm-stats every 5 seconds.
     [:cmd/send {:to :server/scheduler-cmp
                 :msg [:cmd/schedule-new
                       {:timeout 5000 :id :disp-stats :message [:cmd/get-jvm-stats] :repeat true}]}]])

  (pid/save "example.pid")
  (log/info "Application started, PID" (pid/current)))
