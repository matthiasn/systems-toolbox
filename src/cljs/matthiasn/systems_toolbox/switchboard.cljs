(ns matthiasn.systems-toolbox.switchboard
  (:require [cljs.core.match :refer-macros [match]]
            [cljs.core.async :refer [put! sub tap]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.reagent :as r]
            [matthiasn.systems-toolbox.sente :as ws]))

(defn make-comp
  [app cfg]
  (let [{:keys [cmp-id mk-state-fn handler-fn state-pub-handler-fn opts]} cfg
        cmp (comp/make-component mk-state-fn handler-fn state-pub-handler-fn opts)]
    (swap! app assoc-in [:components cmp-id] cmp)
    (prn (:components @app))))

(defn subscribe-component
  "Subscribe component to a specified publisher."
  [app params]
  (let [pub-comp ((:pub-comp params) (:components @app))
        sub-comp ((:sub-comp params) (:components @app))]
    (sub (:state-pub pub-comp) :app-state (:sliding-in-chan sub-comp))
    (swap! app update-in [:subs] conj params)
    (prn (:subs @app))))

(defn tap-comp
  "Tap component channel to a specified mult."
  [app params]
  (let [mult-comp ((:mult-comp params) (:components @app))
        tap-comp  ((:tap-comp params)  (:components @app))]
    (tap (:out-mult mult-comp) (:in-chan tap-comp))
    (swap! app update-in [:taps] conj params)
    (prn (:taps @app))))

(defn make-ws-comp
  "Initializes Sente / WS component and makes is accessible under [:components :ws]
  inside the switchboard state atom."
  [app]
  (let [ws (ws/component)]
    (swap! app assoc-in [:components :ws] ws)))

(defn make-reagent-comp
  "Creates a Reagent component"
  [app params]
  (let [{:keys [cmp-id view-fn dom-id]} params
        cmp (r/component view-fn dom-id)]
    (swap! app assoc-in [:components cmp-id] cmp)))

(defn make-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:components {}
                   :subs #{}
                   :taps #{}})]
    app))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app _ msg]

  (match msg
         [:cmd/make-comp      cmp] (make-comp app cmp)
         [:cmd/make-ws-comp      ] (make-ws-comp app)
         [:cmd/make-r-comp params] (make-reagent-comp app params)
         [:cmd/sub-comp    params] (subscribe-component app params)
         [:cmd/tap-comp    params] (tap-comp app params)
         :else (prn "unknown msg in switchboard-in-loop" msg)))

(defn component
  "Creates a switchboard component that wires individual components together into
  a communicating system."
  []
  (let []
    (comp/make-component make-state in-handler nil)))
