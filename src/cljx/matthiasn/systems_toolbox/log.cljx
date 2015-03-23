(ns matthiasn.systems-toolbox.log
  #+clj (:gen-class)
  (:require [matthiasn.systems-toolbox.component :as comp]))

#+cljs (enable-console-print!)

(defn mk-state
  "Return clean initial component state atom."
  [_]
  (atom {}))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [_ _ msg]
  (println msg))

(defn component
  []
  (comp/make-component :log-cmp mk-state in-handler nil))
