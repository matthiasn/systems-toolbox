(ns matthiasn.systems-toolbox.system

  "A dummy system for use in tests. Has:
    * a switchboard
    * a ping component
    * a pong component
    * message payloads are functions; they typically deliver promises
    * an optional spy component that listens for messages"

  (:require [matthiasn.systems-toolbox.switchboard :as switchboard]
    #?(:clj [clojure.tools.logging :as log]
      :cljs [matthiasn.systems-toolbox.log :as log])))

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
  (let [echo-switchboard (switchboard/component :test/my-switchboard)]
    (switchboard/send-mult-cmd
      echo-switchboard
      ;; Init/wire components
      [[:cmd/init-comp (ping-cmp-map :test/ping-cmp)]
       [:cmd/init-comp (pong-cmp-map :test/pong-cmp)]
       ;; Set up switchboard routes
       [:cmd/route {:from :test/ping-cmp :to :test/pong-cmp}]])

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
