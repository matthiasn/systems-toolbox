(ns matthiasn.systems-toolbox.scheduler-test

  (:require [clojure.test :refer [deftest testing is run-tests]]
            [matthiasn.systems-toolbox.scheduler :as scheduler]
            [matthiasn.systems-toolbox.system :as system]
            [matthiasn.systems-toolbox.switchboard :as switchboard]))

(def echo-switchboard (system/create))

;; Add a scheduler to our system and wire it to pong component
(switchboard/send-mult-cmd
  echo-switchboard
  [[:cmd/init-comp (scheduler/cmp-map :scheduler-cmp)]
   [:cmd/route {:from :scheduler-cmp :to :pong-cmp}]])

(deftest scheduler-cycle
  (let [pong (promise)
        counter (atom 0)
        countdown #(if (< @counter 3)
                    (swap! counter inc)
                    (deliver pong true))
        spy (promise)
        stopped #(deliver spy true)]

    ;; Scheduling
    (switchboard/send-cmd
      echo-switchboard
      [:cmd/send {:to  :scheduler-cmp
                  :msg [:cmd/schedule-new
                        {:timeout 10
                         :id      :cycle
                         :message [:cmd/pong countdown]
                         :repeat  true}]}])
    ;; Scheduling spins ok
    (is (deref pong 1000 false))

    (switchboard/send-mult-cmd
      echo-switchboard
      ;; Listen for scheduler being stopped
      [[:cmd/init-comp (system/spy-cmp-map :spy-cmp
                                           [:info/deleted-timer]
                                           stopped)]
       [:cmd/route {:from :scheduler-cmp :to :spy-cmp}]
       ;; Stop the scheduler
       [:cmd/send {:to  :scheduler-cmp
                   :msg [:cmd/schedule-delete {:id :cycle}]}]])

    (is (deref spy 1000 false))))
