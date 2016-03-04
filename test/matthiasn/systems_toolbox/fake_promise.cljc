(ns matthiasn.systems-toolbox.fake-promise
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [matthiasn.systems-toolbox.component :as cmp]
   #?(:clj  [clojure.test :refer [is]]
      :cljs [cljs.test :refer-macros [async is]])
   #?(:clj  [clojure.core.async :refer [go alts! <!! timeout]]
      :cljs [cljs.core.async :refer [alts! take! timeout]])))

; from http://stackoverflow.com/questions/30766215/how-do-i-unit-test-clojure-core-async-go-macros
(defn test-async
  "Asynchronous test awaiting ch to produce a value or close."
  [ch]
  #?(:clj (<!! ch)
     :cljs (async done (take! ch (fn [_] (done))))))

(defn test-within
  "Asserts that ch does not close or produce a value within ms. Returns a
  channel from which the value can be taken."
  [ms ch]
  (go (let [t (timeout ms)
            [v ch] (alts! [ch t])]
        (is (not= ch t)
            (str "Test should have finished within " ms "ms."))
        v)))

(defn w-timeout
  [ms ch]
  (test-async
    (test-within ms ch)))

