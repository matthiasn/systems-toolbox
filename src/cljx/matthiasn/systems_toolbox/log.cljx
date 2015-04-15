(ns matthiasn.systems-toolbox.log
  #+clj (:gen-class)
  (:require [matthiasn.systems-toolbox.component :as comp]
            #+clj [clojure.tools.logging :as log]))

#+cljs (enable-console-print!)

(defn mk-state
  "Return clean initial component state atom."
  [_]
  (atom {}))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [_ _ msg]
  #+clj (log/info (meta msg) msg)
  #+cljs (println "Log: " (meta msg) " " msg))

(defn component
  []
  (comp/make-component :log-cmp mk-state in-handler nil))
