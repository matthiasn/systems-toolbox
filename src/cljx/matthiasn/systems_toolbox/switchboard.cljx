(ns matthiasn.systems-toolbox.switchboard
  #+clj (:gen-class)
  (:require
    #+clj [clojure.core.match :refer [match]]
    #+cljs [cljs.core.match :refer-macros [match]]
    #+clj [clojure.core.async :refer [put! sub tap]]
    #+cljs [cljs.core.async :refer [put! sub tap]]
    [matthiasn.systems-toolbox.component :as comp]
    [matthiasn.systems-toolbox.log :as l]))

(defn wire-comp
  "Wire existing and already instantiated component."
  [app put-fn cmp]
  (let [cmp-id (:cmp-id cmp)]
    (put-fn [:log/switchboard-wire cmp-id])
    (swap! app assoc-in [:components cmp-id] cmp)))

(defn subscribe
  "Subscribe component to a specified publisher."
  [app put-fn [from-cmp from-pub] msg-type [to-cmp to-chan]]
  (let [pub-comp (from-cmp (:components @app))
        sub-comp (to-cmp (:components @app))]
    (sub (from-pub pub-comp) msg-type (to-chan sub-comp))
    (put-fn [:log/switchboard-sub (str from-cmp " -[" msg-type "]-> " to-cmp)])
    (swap! app update-in [:subs] conj [from-cmp to-cmp])))

(defn subscribe-comp-state
  "Subscribe component to a specified publisher."
  [app put-fn [from to]]
  (doseq [t (flatten [to])]
    (subscribe app put-fn [from :state-pub] :app-state [t :sliding-in-chan])))

(defn subscribe-comp
  "Subscribe component to a specified publisher."
  [app put-fn [from msg-type to]]
  (doseq [t (flatten [to])] (subscribe app put-fn [from :out-pub] msg-type [t :in-chan])))

(defn tap-components
  "Tap into a mult."
  [app put-fn [from-cmps to]]
  (doseq [from (flatten [from-cmps])]
    (let [mult-comp (from (:components @app))
          tap-comp  (to  (:components @app))]
      (tap (:out-mult mult-comp) (:in-chan tap-comp))
      (put-fn [:log/switchboard-tap (str from " -> " to)])
      (swap! app update-in [:taps] conj [from to]))))

(defn make-log-comp
  "Creates a log component."
  [app put-fn]
  (let [log-comp (l/component)]
    (swap! app assoc-in [:components :log] log-comp)
    (put-fn [:log/switchboard-init :log])
    log-comp))

(defn- self-register
  "Registers switchboard itself as another component that can be wired. Useful
  for communication with the outside world / within hierarchies where a subsystem
  has its own switchboard."
  [app put-fn self]
  (swap! app assoc-in [:components :switchboard] self))

(defn make-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:components {} :subs #{} :taps #{}})]
    app))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:cmd/self-register     self] (self-register app put-fn self)
         [:cmd/wire-comp          cmp] (wire-comp app put-fn cmp)
         [:cmd/make-log-comp         ] (make-log-comp app put-fn)
         [:cmd/sub-comp-state from-to] (subscribe-comp-state app put-fn from-to)
         [:cmd/sub-comp       from-to] (subscribe-comp app put-fn from-to)
         [:cmd/tap-comp       from-to] (tap-components app put-fn from-to)
         :else (prn "unknown msg in switchboard-in-loop" msg)))

(defn component
  "Creates a switchboard component that wires individual components together into
  a communicating system."
  []
  (println "Switchboard starting.")
  (let [switchboard (comp/make-component :switchboard make-state in-handler nil)
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
