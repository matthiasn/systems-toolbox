(ns matthiasn.systems-toolbox.log
  (:require [clojure.string :as s]))

(defn warn
  "Print platform-specific warning."
  [& args]
  (prn (str "WARN: " (s/join " " args))))

(defn debug
  "Print platform-specific warning."
  [& args]
  (prn (str "DEBUG: " (s/join " " args))))
