(ns matthiasn.systems-toolbox.metrics
  (:gen-class)
  (:import [java.lang.management ManagementFactory
                                 OperatingSystemMXBean
                                 MemoryMXBean
                                 MemoryUsage
                                 GarbageCollectorMXBean
                                 RuntimeMXBean])
  (:require
    [clojure.tools.logging :as log]
    [clojure.core.match :refer [match]]
    [matthiasn.systems-toolbox.component :as comp]))

(def ^OperatingSystemMXBean os-mx-bean (ManagementFactory/getOperatingSystemMXBean))
(def ^MemoryMXBean mem-mx-bean (ManagementFactory/getMemoryMXBean))
(def ^GarbageCollectorMXBean gc-mx-bean (first (ManagementFactory/getGarbageCollectorMXBeans)))
(def ^RuntimeMXBean rt-mx-bean (ManagementFactory/getRuntimeMXBean))

(defn mk-state
  "Return clean initial component state."
  [put-fn]
  (let [state (atom {})]
    state))

(defn system-utilization
  []
  (let [^MemoryUsage mem-usage (.getHeapMemoryUsage mem-mx-bean)]
    {:system-load-avg (.getSystemLoadAverage os-mx-bean)
     :available-cpus (.getAvailableProcessors os-mx-bean)
     :heap-used (.getUsed mem-usage)
     :heap-max (.getMax mem-usage)
     :gc-count (.getCollectionCount gc-mx-bean)
     :gc-time (.getCollectionTime gc-mx-bean)
     :uptime (.getUptime rt-mx-bean)
     :start-time (.getStartTime rt-mx-bean)}))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [state put-fn msg]
  (match msg
         [:cmd/get-jvm-stats] (put-fn [:stats/jvm (system-utilization)])
         :else (log/error "unknown msg in component" msg)))

(defn component
  [cmp-id]
  (comp/make-component cmp-id mk-state in-handler nil))
