(ns matthiasn.systems-toolbox.log
  "Some helpers for logging in ClojureScript"
  (:require [clojure.string :as s]
            [matthiasn.systems-toolbox.component.helpers :as h]))

(def ^{:dynamic true} *debug-enabled* false)
(defn enable-debug-log! [] (set! *debug-enabled* true))

(defn info [& args]
  (println (s/join " " args)))

(defn warn [& args]
  (println (str "WARN: " (s/join " " args))))

(defn error [& args]
  (println (str "ERROR: " (s/join " " args))))

(defn debug [& args]
  (when *debug-enabled*
    (println (str (h/now) " DEBUG: " (s/join " " args)))))
