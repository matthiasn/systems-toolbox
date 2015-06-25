(ns matthiasn.systems-toolbox.log
  #+clj (:gen-class)
  (:require [matthiasn.systems-toolbox.component :as comp]
            #+clj [clojure.tools.logging :as log]))

#+cljs (enable-console-print!)

(defn all-msgs-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [msg msg-meta]}]
  #+clj (log/info msg-meta msg)
  #+cljs (println "Log: " msg-meta " " msg))

(defn component
  "Creates component for logging, which in this case does not need local state."
  [cmp-id]
  (comp/make-component
    {:cmp-id  cmp-id
     :all-msgs-handler all-msgs-handler
     :msgs-on-firehose false
     :snapshots-on-firehose false}))
