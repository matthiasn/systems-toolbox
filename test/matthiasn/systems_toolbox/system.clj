(ns matthiasn.systems-toolbox.system

  "A dummy system for use in tests. Has:
    * a switchboard
    * a ping component
    * a pong component
    * message payloads are functions; they typically deliver promises
    * an optional spy component that listen for messages"

  (:require [clojure.tools.logging :as log]
            [matthiasn.systems-toolbox.switchboard :as switchboard]))

;; Ping component

(defn ping-handler [{:keys [put-fn msg-payload]}]
  (log/debug "Ping!")
  (put-fn [:cmd/pong (msg-payload)]))

(defn ping-cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:cmd/ping ping-handler}})

;; Pong component

(defn pong-handler [{:keys [msg-payload]}]
  (log/debug "Pong!")
  (msg-payload))

(defn pong-cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:cmd/pong pong-handler}})

;; System (= switchboard + components + wiring + routing)

(defn create []
  (let [echo-switchboard (switchboard/component :my-switchboard)]
    (switchboard/send-mult-cmd
      echo-switchboard
      ;; Init/wire components
      [[:cmd/init-comp (ping-cmp-map :ping-cmp)]
       [:cmd/init-comp (pong-cmp-map :pong-cmp)]
       ;; Set up switchboard routes
       [:cmd/route {:from :ping-cmp :to :pong-cmp}]])

    echo-switchboard))

(defn spy-cmp-map
  "Optional spy component
  - types argument is a list of messages we want to spy
  - f is called in the handler"
  [cmp-id types f]
  (let [handler (fn [{:keys [msg-payload]}]
                  (log/debug "Spy:" msg-payload)
                  (f))
        handler-map (->> types
                         (map #(vector %1 handler))
                         (into {}))]
    {:cmp-id      cmp-id
     :handler-map handler-map}))
