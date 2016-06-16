(ns matthiasn.systems-toolbox.switchboard-route-tests
  "Here, we test the route and route-all wiring between components."
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require  [matthiasn.systems-toolbox.test-spec]
    #?(:clj  [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
    #?(:clj  [clojure.core.async :refer [go]])
             [matthiasn.systems-toolbox.switchboard :as sb]
             [matthiasn.systems-toolbox.test-promise :as tp]))

(defn sender-cmp-map
  "Map for component that receives messages and immediately emits them again."
  [cmp-id]
  {:cmp-id           cmp-id
   :all-msgs-handler (fn [{:keys [msg]}] {:emit-msg msg})})

(defn recipient-cmp-map
  "Map for component that receives messages and changes the provided state by calculating sum of accumulated
  value in state and incoming messages. This allows for checking if all messages were received."
  [cmp-id state-atom]
  {:cmp-id            cmp-id
   :handler-map       {:test/sum (fn [{:keys [current-state msg-payload]}]
                                   {:new-state (update-in current-state [:sum] + (:n msg-payload))})}
   :all-msgs-handler  (fn [{:keys [current-state msg-payload]}]
                        (when msg-payload
                          {:new-state (update-in current-state [:all-msg-handler-sum] + (:n msg-payload))}))
   :unhandled-handler (fn [{:keys [current-state msg-payload]}]
                        (when msg-payload
                          {:new-state (update-in current-state [:unhandled-handler-sum] + (:n msg-payload))}))
   :state-fn          (fn [_put-fn] {:state state-atom})})


;; Tests for components connected via :cmd/route. This command connects two components, but only for message
;; types for which the recipient has a handler. Other message types are not relayed.
(deftest route-1-1-test
  "One sender, one receiver, 100 messages of handled type and 100 messages of unhandled type sent.
  Expectation: handler receives all, as does all-msgs-handler. Unhandled-handler receives none."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        test-pred #(= (:sum %) 5050)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(sender-cmp-map :test/send-cmp-1)
                         (recipient-cmp-map :test/recv-cmp-1 recv-state-1)}]
       [:cmd/route {:from :test/send-cmp-1 :to :test/recv-cmp-1}]])
    (doseq [n (range 1 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/sum {:n n}]}]))
    (testing "receives all handled messages"
      (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
      (is (test-pred @recv-state-1)))
    (testing "only handled messages were routed and received by all-msgs-handler"
      (is (= (:all-msg-handler-sum @recv-state-1) 5050)))
    (testing "unhandled messages were not routed"
      (is (= (:unhandled-handler-sum @recv-state-1) 0)))))

(deftest route-2-1-test
  "Two senders, one receiver, 100 messages of handled type and 100 messages of unhandled type sent from each sender.
  Expectation: handler receives all, as does all-msgs-handler. Unhandled-handler receives none."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        test-pred #(= (:sum %) 10100)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(sender-cmp-map :test/send-cmp-1)
                         (sender-cmp-map :test/send-cmp-2)
                         (recipient-cmp-map :test/recv-cmp-1 recv-state-1)}]
       [:cmd/route {:from #{:test/send-cmp-1 :test/send-cmp-2} :to :test/recv-cmp-1}]])
    (doseq [n (range 1 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/sum {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-2 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-2 :msg [:test/sum {:n n}]}]))
    (testing "receives all handled messages"
      (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
      (is (test-pred @recv-state-1)))
    (testing "only handled messages were routed and received by all-msgs-handler"
      (is (= (:all-msg-handler-sum @recv-state-1) 10100)))
    (testing "unhandled messages were not routed"
      (is (= (:unhandled-handler-sum @recv-state-1) 0)))))

(deftest route-2-3-test
  "Two senders, three receivers, 100 messages of handled type and 100 messages of unhandled type sent from each sender.
  Expectation for each receiver: handler receives all, as does all-msgs-handler. Unhandled-handler receives none."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        recv-state-2 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        recv-state-3 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        test-pred #(= (:sum %) 10100)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(sender-cmp-map :test/send-cmp-1)
                         (sender-cmp-map :test/send-cmp-2)
                         (recipient-cmp-map :test/recv-cmp-1 recv-state-1)
                         (recipient-cmp-map :test/recv-cmp-2 recv-state-2)
                         (recipient-cmp-map :test/recv-cmp-3 recv-state-3)}]
       [:cmd/route {:from #{:test/send-cmp-1 :test/send-cmp-2}
                    :to   #{:test/recv-cmp-1 :test/recv-cmp-2 :test/recv-cmp-3}}]])
    (doseq [n (range 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/sum {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-2 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-2 :msg [:test/sum {:n n}]}]))
    (testing "receives all handled messages"
      (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
      (is (test-pred @recv-state-1))
      (is (test-pred @recv-state-2))
      (is (test-pred @recv-state-3)))
    (testing "only handled messages were routed and received by all-msgs-handler"
      (is (= (:all-msg-handler-sum @recv-state-1) 10100))
      (is (= (:all-msg-handler-sum @recv-state-2) 10100))
      (is (= (:all-msg-handler-sum @recv-state-3) 10100)))
    (testing "unhandled messages were not routed and thus not received"
      (is (= (:unhandled-handler-sum @recv-state-1) 0))
      (is (= (:unhandled-handler-sum @recv-state-2) 0))
      (is (= (:unhandled-handler-sum @recv-state-3) 0)))))


;; Tests for components connected via :cmd/routeall. This command connects two components, no matter if the recipient
;; has a handler defined for a particular message type. In case there's no handler defined, the unhandled-handler
;; will receive the message. The all-msgs-handler will receive all messages.
(deftest route-all-1-1-test
  "One sender, one receiver, 100 messages of handled type and 100 messages of unhandled type sent.
  Expectation: handler receives all handled messages, all-msgs-handler receives all.
  Unhandled-handler receives all messages for which there's no handler."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        test-pred #(= (:sum %) 5050)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(sender-cmp-map :test/send-cmp-1)
                         (recipient-cmp-map :test/recv-cmp-1 recv-state-1)}]
       [:cmd/route-all {:from :test/send-cmp-1 :to :test/recv-cmp-1}]])
    (doseq [n (range 1 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/sum {:n n}]}]))
    (testing "receives all handled messages"
      (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
      (is (test-pred @recv-state-1)))
    (testing "all-msgs-handler receives both handled and unhandled messages"
      (is (= (:all-msg-handler-sum @recv-state-1) 10100)))
    (testing "unhandled messages were routed and received by unhandled-handler"
      (is (= (:unhandled-handler-sum @recv-state-1) 5050)))))

(deftest route-2-1-test
  "Two senders, one receiver, 100 messages of handled type sent from each sender.
  Expectation: handler receives all handled message. All-msgs-handler receives all.
  Unhandled-handler receives all messages for which there's no handler."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        test-pred #(= (:sum %) 10100)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(sender-cmp-map :test/send-cmp-1)
                         (sender-cmp-map :test/send-cmp-2)
                         (recipient-cmp-map :test/recv-cmp-1 recv-state-1)}]
       [:cmd/route-all {:from #{:test/send-cmp-1 :test/send-cmp-2} :to :test/recv-cmp-1}]])
    (doseq [n (range 1 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/sum {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-2 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-2 :msg [:test/sum {:n n}]}]))
    (testing "receives all handled messages"
      (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
      (is (test-pred @recv-state-1)))
    (testing "all-msgs-handler receives both handled and unhandled messages"
      (is (= (:all-msg-handler-sum @recv-state-1) 20200)))
    (testing "unhandled messages were routed and received by unhandled-handler"
      (is (= (:unhandled-handler-sum @recv-state-1) 10100)))))

(deftest route-all-2-3-test
  "Two senders, three receivers, 100 messages of handled type sent from each sender.
  Expectation for each receiver: handler receives all, as does all-msgs-handler.
  Unhandled-handler receives none."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        recv-state-2 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        recv-state-3 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        test-pred #(= (:sum %) 10100)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(sender-cmp-map :test/send-cmp-1)
                         (sender-cmp-map :test/send-cmp-2)
                         (recipient-cmp-map :test/recv-cmp-1 recv-state-1)
                         (recipient-cmp-map :test/recv-cmp-2 recv-state-2)
                         (recipient-cmp-map :test/recv-cmp-3 recv-state-3)}]
       [:cmd/route {:from #{:test/send-cmp-1 :test/send-cmp-2}
                    :to   #{:test/recv-cmp-1 :test/recv-cmp-2 :test/recv-cmp-3}}]])
    (doseq [n (range 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-1 :msg [:test/sum {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-2 :msg [:test/unhandled {:n n}]}])
      (sb/send-cmd switchboard [:cmd/send {:to :test/send-cmp-2 :msg [:test/sum {:n n}]}]))
    (testing "receives all handled messages"
      (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
      (is (test-pred @recv-state-1))
      (is (test-pred @recv-state-2))
      (is (test-pred @recv-state-3)))
    (testing "all-msgs-handler receives both handled and unhandled messages"
      (is (= (:all-msg-handler-sum @recv-state-1) 10100))
      (is (= (:all-msg-handler-sum @recv-state-2) 10100))
      (is (= (:all-msg-handler-sum @recv-state-3) 10100)))
    (testing "unhandled messages were routed and received by unhandled-handler"
      (is (= (:unhandled-handler-sum @recv-state-1) 0))
      (is (= (:unhandled-handler-sum @recv-state-2) 0))
      (is (= (:unhandled-handler-sum @recv-state-3) 0)))))

