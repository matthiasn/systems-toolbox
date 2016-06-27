(ns example.metrics
  (:require [example.spec]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-metrics.jvmstats :as jvmstats]))

(defn init!
  "Initialize display of some JVM metrics in the UI by firing up component and
  then wiring messages as needed."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (jvmstats/cmp-map :client/jvmstats-cmp
                                       {:dom-id           "jvm-stats-frame"
                                        :msgs-on-firehose true})]
     [:cmd/route {:from :client/ws-cmp
                  :to   :client/jvmstats-cmp}]]))
