(ns matthiasn.systems-toolbox.system-test

  "Create a system, send some messages, see them flowing correctly."
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [matthiasn.systems-toolbox.test-spec]
            [matthiasn.systems-toolbox.system :as system]
            [matthiasn.systems-toolbox.switchboard :as switchboard]
   #?(:clj  [clojure.test :refer [deftest testing is]]
      :cljs [cljs.test :refer-macros [deftest testing is]])
   #?(:clj  [clojure.core.async :refer [<! put! go promise-chan]]
      :cljs [cljs.core.async :refer [<! put! promise-chan]])
            [matthiasn.systems-toolbox.test-promise :as tp]))

(deftest message-flow
  (let [all-recvd (promise-chan)
        repetitions 100
        ping-state (atom {:n 0 :expected-cnt repetitions :all-recvd all-recvd})
        pong-state (atom {:n 0})
        echo-switchboard (system/create ping-state pong-state)]

    (dotimes [_ repetitions]
      (switchboard/send-cmd
        echo-switchboard
        [:cmd/send {:to  :test/pong-cmp
                    :msg [:cmd/ping]}]))

    (tp/w-timeout 1000 (go
                         (testing "all messages received"
                           (is (true? (<! all-recvd))))
                         (testing "sent messages equal received messages"
                           (is (= repetitions (:n @ping-state) (:n @pong-state))))))))
