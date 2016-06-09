(ns matthiasn.systems-toolbox.log)

(defn warn
  "Print platform-specific warning."
  [& args]
  (apply prn "WARN:" args))
