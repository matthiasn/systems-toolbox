(ns matthiasn.systems-toolbox.scheduler-test

  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require
    #?(:clj [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
    #?(:clj [clojure.core.async :refer [<! put! go promise-chan]]
       :cljs [cljs.core.async :refer [<! put! promise-chan]])
            [matthiasn.systems-toolbox.scheduler :as scheduler]
            [matthiasn.systems-toolbox.system :as system]
            [matthiasn.systems-toolbox.switchboard :as switchboard]
            [matthiasn.systems-toolbox.test-promise :as tp]))

(def echo-switchboard (system/create))

;; Add a scheduler to our system and wire it to pong component
(switchboard/send-mult-cmd
  echo-switchboard
  [[:cmd/init-comp (scheduler/cmp-map :scheduler-cmp)]
   [:cmd/route {:from :scheduler-cmp :to :pong-cmp}]])

(deftest scheduler-cycle
  (let [pong (promise-chan)
        counter (atom 0)
        countdown #(if (< @counter 3)
                    (swap! counter inc)
                    (put! pong true))
        spy (promise-chan)
        stopped #(put! spy true)]

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
    (tp/w-timeout 1000 (go (is (true? (<! pong)))))

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

    (tp/w-timeout 1000 (go (is (true? (<! spy)))))))
