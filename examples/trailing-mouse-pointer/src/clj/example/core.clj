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

(def switchboard (sb/component))

(defn -main [& args]
  (pid/save "example.pid")
  (pid/delete-on-shutdown! "example.pid")

  (sb/send-mult-cmd
    switchboard
    [[:cmd/wire-comp (sente/component :ws-cmp index/index-page 8010)] ; Component for WebSocket communication
     [:cmd/wire-comp (sched/component :scheduler-cmp)]  ; Component for scheduling the dispatch of messages
     [:cmd/wire-comp (ptr/component :ptr-cmp)]          ; Component for processing mouse moves
     [:cmd/wire-comp (metrics/component :metrics-cmp)]  ; Component for metrics and stats

     #_[:cmd/tap-comp
      [:ws-cmp         ;    »───»───»──╢   Route all incoming WebSockets messages to the implicitly
       :log-cmp]]      ; <= «═══«═══«══╝   instantiated logging component. Only used for development purposes.

     [:cmd/tap-comp
      [:ptr-cmp    ;    »───»───»──╢   Route all messages from :pointer-cmp to :ws-cmp.
       :ws-cmp]]   ; <= «═══«═══«══╝

     [:cmd/sub-comp
      [[:ws-cmp :cmd/mouse-pos]] ;    »─[:cmd/mouse-pos]─»──»─╢  :webdriver-cmp subscribes to mouse positions.
      :ptr-cmp]                  ; <= «═══«═══«═══«═══«═══«═══╝

     [:cmd/sub-comp
      [[:metrics-cmp :stats/jvm]] ;    »─[:stats/jvm]»─╢   :ws-cmp subscribes to metrics about JVM / runtime.
      :ws-cmp]                    ; <= «═══«═══«═══«══«╝

      [:cmd/sub-comp
      [[:scheduler-cmp :cmd/get-jvm-stats]] ;    »─[:cmd/get-jvm-stats]─»─╢  :metrics-cmp listens to trigger msg.
      :metrics-cmp]                         ; <= «═══«═══«═══«═══«═══«═══«╝

     [:cmd/send-to
      [:scheduler-cmp                       ; Schedule dispatch of :cmd/get-jvm-stats every 5 seconds.
       [:cmd/schedule-new {:timeout 5000
                           :id :cmd/get-jvm-stats
                           :message [:cmd/get-jvm-stats]
                           :repeat true}]]]])

  (pid/save "example.pid")
  (log/info "Application started, PID" (pid/current)))
