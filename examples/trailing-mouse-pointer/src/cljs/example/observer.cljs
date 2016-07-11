(ns example.observer
  (:require [example.spec]
            [example.conf :as conf]
            [matthiasn.systems-toolbox-ui.charts.observer :as obs]
            [matthiasn.systems-toolbox.switchboard :as sb]))

(defn init!
  "Initialize and wire Observer component."
  [switchboard]
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp (obs/cmp-map :client/observer-cmp conf/observer-cfg-map)]
     [:cmd/attach-to-firehose :client/observer-cmp]
     ; let WebSockets component receive all messages from firehose
     ;[:cmd/attach-to-firehose :client/ws-cmp]
     ]))
