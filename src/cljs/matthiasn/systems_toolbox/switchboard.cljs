(ns matthiasn.systems-toolbox.switchboard
  (:require [cljs.core.match :refer-macros [match]]
            [cljs.core.async :refer [put! sub tap]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.reagent :as r]
            [matthiasn.systems-toolbox.log :as l]
            [matthiasn.systems-toolbox.sente :as ws]))

(defn make-comp
  [app put-fn cfg]
  (let [{:keys [cmp-id mk-state-fn handler-fn state-pub-handler-fn opts]} cfg
        cmp (comp/make-component mk-state-fn handler-fn state-pub-handler-fn opts)]
    (put-fn [:log/switchboard-init cmp-id])
    (swap! app assoc-in [:components cmp-id] cmp)))

(defn subscribe-component
  "Subscribe component to a specified publisher."
  [app put-fn [from to]]
  (let [pub-comp (from (:components @app))
        sub-comp (to (:components @app))]
    (sub (:state-pub pub-comp) :app-state (:sliding-in-chan sub-comp))
    (put-fn [:log/switchboard-sub (str from "->" to)])
    (swap! app update-in [:subs] conj [from to])))

(defn tap-comp
  "Tap component in channel to a specified mult."
  [app put-fn [from to]]
  (let [mult-comp (from (:components @app))
        tap-comp  (to  (:components @app))]
    (tap (:out-mult mult-comp) (:in-chan tap-comp))
    (put-fn [:log/switchboard-tap (str from "->" to)])
    (swap! app update-in [:taps] conj [from to])))

(defn make-ws-comp
  "Initializes Sente / WS component and makes is accessible under [:components :ws]
  inside the switchboard state atom."
  [app put-fn]
  (let [ws (ws/component)]
    (swap! app assoc-in [:components :ws] ws)
    (put-fn [:log/switchboard-init :ws])
    ws))

(defn make-reagent-comp
  "Creates a Reagent component."
  [app put-fn params]
  (let [{:keys [cmp-id view-fn dom-id init-state]} params
        cmp (r/component view-fn dom-id init-state)]
    (put-fn [:log/switchboard-init-reagent cmp-id])
    (swap! app assoc-in [:components cmp-id] cmp)))

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
         [:cmd/make-comp      cmp] (make-comp app put-fn cmp)
         [:cmd/make-ws-comp      ] (make-ws-comp app put-fn)
         [:cmd/make-log-comp     ] (make-log-comp app put-fn)
         [:cmd/make-r-comp params] (make-reagent-comp app put-fn params)
         [:cmd/sub-comp   from-to] (subscribe-component app put-fn from-to)
         [:cmd/tap-comp   from-to] (tap-comp app put-fn from-to)
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

(defn send
  "Send message to the specified switchboard component."
  [switchboard msg]
  (put! (:in-chan switchboard) msg))
