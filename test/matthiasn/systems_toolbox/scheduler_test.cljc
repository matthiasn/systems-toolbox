(ns matthiasn.systems-toolbox.scheduler-test

  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require  [matthiasn.systems-toolbox.test-spec]
    #?(:clj  [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
    #?(:clj  [clojure.core.async :refer [<! put! go promise-chan]]
       :cljs [cljs.core.async :refer [<! put! promise-chan]])
             [matthiasn.systems-toolbox.scheduler :as scheduler]
             [matthiasn.systems-toolbox.system :as system]
             [matthiasn.systems-toolbox.switchboard :as switchboard]
             [matthiasn.systems-toolbox.test-promise :as tp]))

;; Add a scheduler to our system and wire it to pong component
(deftest scheduler-cycle
  (let [all-recvd (promise-chan)
        ping-state (atom {:n 0 :expected-cnt 100 :all-recvd all-recvd})
        pong-state (atom {:n 0})
        echo-switchboard (system/create ping-state pong-state)
        spy (promise-chan)
        stopped #(put! spy true)]
    (switchboard/send-mult-cmd
      echo-switchboard
      [[:cmd/init-comp (scheduler/cmp-map :test/scheduler-cmp)]
       [:cmd/route {:from :test/scheduler-cmp :to :test/pong-cmp}]])
    ;; Scheduling
    (switchboard/send-cmd
      echo-switchboard
      [:cmd/send {:to  :test/scheduler-cmp
                  :msg [:cmd/schedule-new
                        {:timeout 1
                         :id      :cycle
                         :message [:cmd/ping]
                         :repeat  true}]}])
    ;; Scheduling spins ok
    (tp/w-timeout 1000 (go
                         (testing "all messages received"
                           (is (true? (<! all-recvd))))
                         (testing "sent messages equal received messages"
                           (is (>= (:n @pong-state) 100)))))

    (switchboard/send-mult-cmd
      echo-switchboard
      ;; Listen for scheduler being stopped
      [[:cmd/init-comp (system/spy-cmp-map :test/spy-cmp [:info/deleted-timer] stopped)]
       [:cmd/route {:from :test/scheduler-cmp :to :test/spy-cmp}]
       ;; Stop the scheduler
       [:cmd/send {:to  :test/scheduler-cmp
                   :msg [:cmd/schedule-delete {:id :cycle}]}]])

    (tp/w-timeout 1000 (go (is (true? (<! spy)))))))
