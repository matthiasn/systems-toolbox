(ns matthiasn.systems-toolbox.log
  (:require #?(:clj [clojure.tools.logging :as log])))

(defn warn
  "Print platform-specific warning."
  [& args]
  #?(:clj  (log/warn args)
     :cljs (prn "WARN:" args)))

(defn info
  "Print platform-specific warning."
  [& args]
  #?(:clj  (log/info args)
     :cljs (prn "INFO:" args)))
