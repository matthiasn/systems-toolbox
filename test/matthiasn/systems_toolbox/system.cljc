(ns matthiasn.systems-toolbox.system

  "A dummy system for use in tests. Has:
    * a switchboard
    * a ping component
    * a pong component
    * message payloads are functions; they typically deliver promises
    * an optional spy component that listens for messages"

  (:require [matthiasn.systems-toolbox.switchboard :as switchboard]
    #?(:clj [clojure.tools.logging :as log]
      :cljs [matthiasn.systems-toolbox.log :as log])
    #?(:clj  [clojure.core.async :refer [<! put! go promise-chan]]
       :cljs [cljs.core.async :refer [<! put! promise-chan]])))

;; Ping component
(defn pong-handler
  [{:keys [current-state]}]
  (let [new-state (update-in current-state [:n] inc)]
    (when (= (:n new-state) (:expected-cnt new-state))
      (put! (:all-recvd new-state) true))
    {:new-state (update-in current-state [:n] inc)}))

(defn ping-cmp-map [cmp-id cmp-state]
  {:cmp-id      cmp-id
   :state-fn    (fn [_] {:state cmp-state})
   :handler-map {:cmd/pong pong-handler}})

;; Pong component
(defn ping-handler
  [{:keys [current-state]}]
  {:new-state (update-in current-state [:n] inc)
   :emit-msg [:cmd/pong]})

(defn pong-cmp-map [cmp-id cmp-state]
  {:cmp-id      cmp-id
   :state-fn    (fn [_] {:state cmp-state})
   :handler-map {:cmd/ping ping-handler}})

;; System (= switchboard + components + wiring + routing)
(defn create [ping-state pong-state]
  (let [echo-switchboard (switchboard/component :test/my-switchboard)]
    (switchboard/send-mult-cmd
      echo-switchboard
      ;; Init/wire components
      [[:cmd/init-comp #{(ping-cmp-map :test/ping-cmp ping-state)
                         (pong-cmp-map :test/pong-cmp pong-state)}]
       ;; Set up switchboard routes
       [:cmd/route {:from :test/ping-cmp :to :test/pong-cmp}]
       [:cmd/route {:from :test/pong-cmp :to :test/ping-cmp}]])

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
