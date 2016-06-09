(ns matthiasn.systems-toolbox.log
  (:require [clojure.string :as s]))

(defn error
  "Substitute for log/error, can be used in cljs"
  [& args]
  (prn (str "ERROR: " (s/join " " args))))

(defn warn
  "Substitute for log/warn, can be used in cljs"
  [& args]
  (prn (str "WARN: " (s/join " " args))))

(defn debug
  "Substitute for log/debug, can be used in cljs"
  [& args]
  (prn (str "DEBUG: " (s/join " " args))))
