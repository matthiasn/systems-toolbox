(ns matthiasn.systems-toolbox.test-spec
  (:require
    #?(:clj  [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test Specs
(s/def :some/type number?)
(s/def :cmd/ping nil?)
(s/def :cmd/pong nil?)
(s/def :int/div-by-10 #(and (number? %) (zero? (mod % 10))))
(s/def :int/div-by-100 #(and (number? %) (zero? (mod % 100))))

(s/def :test/n number?)
(s/def :test/done nil?)
(s/def :test/sum (s/keys :req-un [:test/n]))
(s/def :test/unhandled :test/sum)
