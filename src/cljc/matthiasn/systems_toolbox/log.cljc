(ns matthiasn.systems-toolbox.log
  (:require [clojure.string :as s]
            [matthiasn.systems-toolbox.component.helpers :as h]))

(defn error
  "Substitute for log/error, can be used in cljs"
  [& args]
  (prn (str "ERROR: " (s/join " " args))))

(defn warn
  "Substitute for log/warn, can be used in cljs"
  [& args]
  (prn (str "WARN: " (s/join " " args))))

(def ^{:dynamic true} *debug-enabled* false)
(defn enable-debug-log! [] (set! *debug-enabled* true))

(defn debug
  "Substitute for log/debug, can be used in cljs"
  [& args]
  (when *debug-enabled*
    (prn (str (h/now) " DEBUG: " (s/join " " args)))))
