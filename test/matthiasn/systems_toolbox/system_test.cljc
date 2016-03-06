(ns matthiasn.systems-toolbox.system-test

  "Create a system, send some messages, see them flowing correctly."
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [matthiasn.systems-toolbox.system :as system]
            [matthiasn.systems-toolbox.switchboard :as switchboard]
   #?(:clj [clojure.test :refer [deftest testing is]]
      :cljs [cljs.test :refer-macros [deftest testing is]])
   #?(:clj [clojure.core.async :refer [<! put! go promise-chan]]
      :cljs [cljs.core.async :refer [<! put! promise-chan]])
            [matthiasn.systems-toolbox.test-promise :as tp]))

(def echo-switchboard (system/create))

(deftest message-flow
  (let [ping (promise-chan)
        pong (promise-chan)
        ;; onion is peeled one layer per component
        onion (fn [] (do (put! ping true)
                         #(put! pong true)))]
    (switchboard/send-cmd
      echo-switchboard
      [:cmd/send {:to  :ping-cmp
                  :msg [:cmd/ping onion]}])

    (tp/w-timeout 1000 (go (is (true? (<! ping)))
                           (is (true? (<! pong)))))))
