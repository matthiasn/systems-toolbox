(ns example.metrics
  (:require [example.spec]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-metrics.metrics :as metrics]
            [matthiasn.systems-toolbox.scheduler :as sched]))

(defn start!
  "Starts collecting and gathering JVM stats."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (sched/cmp-map :server/scheduler-cmp)]
     [:cmd/init-comp (metrics/cmp-map :server/metrics-cmp)]
     [:cmd/route {:from :server/metrics-cmp :to :server/ws-cmp}]
     [:cmd/route {:from :server/scheduler-cmp :to :server/metrics-cmp}]
     [:cmd/send {:to  :server/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 5000
                                          :message [:cmd/get-jvm-stats]
                                          :repeat true}]}]]))
