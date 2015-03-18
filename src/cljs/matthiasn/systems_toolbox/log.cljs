(ns matthiasn.systems-toolbox.log
  (:require [matthiasn.systems-toolbox.component :as comp]))

(enable-console-print!)

(defn make-state
  "Return clean initial component state atom."
  [_]
  (atom {}))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [_ _ msg]
  (prn msg))

(defn component
  []
  (comp/make-component make-state in-handler nil))
