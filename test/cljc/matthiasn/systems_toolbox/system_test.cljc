(ns matthiasn.systems-toolbox.system-test

  "Create a system, send some messages, see them flowing correctly."

  (:require [clojure.test :refer [deftest testing is run-tests]]
            [matthiasn.systems-toolbox.system :as system]
            [matthiasn.systems-toolbox.switchboard :as switchboard]))

(def echo-switchboard (system/create))

(deftest message-flow
  (let [ping (promise)
        pong (promise)
        ;; onion is peeled one layer per component
        onion (fn [] (do (deliver ping true)
                         #(deliver pong true)))]
    (switchboard/send-cmd
      echo-switchboard
      [:cmd/send {:to  :ping-cmp
                  :msg [:cmd/ping onion]}])
    (is (deref ping 1000 false))
    (is (deref pong 1000 false))))
