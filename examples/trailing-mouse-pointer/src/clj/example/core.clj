(ns example.core
  (:require [example.spec]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.server :as sente]
            [matthiasn.systems-toolbox-kafka.kafka-producer2 :as kp2]
            [example.index :as index]
            [clojure.tools.logging :as log]
            [clj-pid.core :as pid]
            [matthiasn.systemd-watchdog.core :as wd]
            [example.pointer :as ptr])
  (:gen-class))

(defonce switchboard (sb/component :server/switchboard))

(defn make-observable [components]
  (if (System/getenv "OBSERVER")
    (let [cfg {:cfg         {:bootstrap-servers "localhost:9092"
                             :auto-offset-reset "latest"
                             :topic             "firehose"}
               :relay-types #{:firehose/cmp-put
                              :firehose/cmp-recv}}
          mapper #(assoc-in % [:opts :msgs-on-firehose] true)
          components (set (mapv mapper components))
          firehose-kafka (kp2/cmp-map :server/kafka-firehose cfg)]
      (conj components firehose-kafka))
    components))

(defn restart!
  "Starts or restarts system by asking switchboard to fire up the provided
   ws-cmp and the ptr component, which handles and counts messages about mouse
   moves."
  []
  (let [components #{(sente/cmp-map :server/ws-cmp index/sente-map)
                     (ptr/cmp-map :server/ptr-cmp)}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]

       [:cmd/route {:from :server/ptr-cmp
                    :to   :server/ws-cmp}]

       [:cmd/route {:from :server/ws-cmp
                    :to   :server/ptr-cmp}]

       (when (System/getenv "OBSERVER")
         [:cmd/attach-to-firehose :server/kafka-firehose])])))

(defn -main [& args]
  (pid/save "example.pid")
  (pid/delete-on-shutdown! "ws-example.pid")
  (log/info "Application started, PID" (pid/current))
  (restart!)
  (wd/start-watchdog! 5000)
  (Thread/sleep Long/MAX_VALUE))
