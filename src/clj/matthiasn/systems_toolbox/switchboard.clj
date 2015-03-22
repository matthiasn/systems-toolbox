(ns matthiasn.systems-toolbox.switchboard
  (:gen-class)
  (:require [clojure.core.match :refer [match]]
            [clojure.core.async :refer [put! sub tap]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.log :as l]))

(defn wire-comp
  [app put-fn [cmp-id cmp]]
  (put-fn [:log/switchboard-wire cmp-id])
  (swap! app assoc-in [:components cmp-id] cmp))

(defn subscribe-component
  "Subscribe component to a specified publisher."
  [app put-fn [from to]]
  (let [pub-comp (from (:components @app))
        sub-comp (to (:components @app))]
    (sub (:state-pub pub-comp) :app-state (:sliding-in-chan sub-comp))
    (put-fn [:log/switchboard-sub (str from "->" to)])
    (swap! app update-in [:subs] conj [from to])))

(defn subscribe-components
  [app put-fn [from to-seq]]
  (doseq [to to-seq]
    (subscribe-component app put-fn [from to])))

(defn tap-component
  "Tap component in channel to a specified mult."
  [app put-fn [from to]]
  (let [mult-comp (from (:components @app))
        tap-comp  (to  (:components @app))]
    (tap (:out-mult mult-comp) (:in-chan tap-comp))
    (put-fn [:log/switchboard-tap (str from "->" to)])
    (swap! app update-in [:taps] conj [from to])))

(defn tap-components
  [app put-fn [from-seq to]]
  (doseq [from from-seq]
    (tap-component app put-fn [from to])))

(defn make-log-comp
  "Creates a log component."
  [app put-fn]
  (let [log-comp (l/component)]
    (swap! app assoc-in [:components :log] log-comp)
    (put-fn [:log/switchboard-init :log])
    log-comp))

(defn self-register
  ""
  [app put-fn self]
  (swap! app assoc-in [:components :switchboard] self))

(defn make-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:components {}
                   :subs #{}
                   :taps #{}})]
    app))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:cmd/self-register self] (self-register app put-fn self)
         [:cmd/wire-comp   params] (wire-comp app put-fn params)
         [:cmd/make-log-comp     ] (make-log-comp app put-fn)
         [:cmd/sub-comp   from-to] (subscribe-component app put-fn from-to)
         [:cmd/tap-comp   from-to] (tap-component app put-fn from-to)
         [:cmd/sub-comps  from-to] (subscribe-components app put-fn from-to)
         [:cmd/tap-comps  from-to] (tap-components app put-fn from-to)
         :else (prn "unknown msg in switchboard-in-loop" msg)))

(defn component
  "Creates a switchboard component that wires individual components together into
  a communicating system."
  []
  (prn "Switchboard starting.")
  (let [switchboard (comp/make-component make-state in-handler nil)
        sw-in-chan (:in-chan switchboard)]
    (put! sw-in-chan [:cmd/self-register switchboard])
    (put! sw-in-chan [:cmd/make-log-comp])
    (put! sw-in-chan [:cmd/tap-comp [:switchboard :log]])
    switchboard))

(defn send-cmd
  "Send message to the specified switchboard component."
  [switchboard cmd]
  (put! (:in-chan switchboard) cmd))

(defn send-mult-cmd
  "Send messages to the specified switchboard component."
  [switchboard cmds]
  (doseq [cmd cmds] (put! (:in-chan switchboard) cmd)))

