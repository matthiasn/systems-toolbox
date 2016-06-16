(ns matthiasn.systems-toolbox.switchboard-observe-tests
  "Here, we test the route and route-all wiring between components."
  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require  [matthiasn.systems-toolbox.test-spec]
    #?(:clj  [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
    #?(:clj  [clojure.core.async :refer [go]])
             [matthiasn.systems-toolbox.switchboard :as sb]
             [matthiasn.systems-toolbox.test-promise :as tp]))


(defn observed-cmp-map
  "Map for component that receives messages and changes the provided state by calculating sum of accumulated
  value in state and incoming messages. This allows for checking if all messages were received."
  [cmp-id state-atom]
  {:cmp-id            cmp-id
   :handler-map       {:test/sum (fn [{:keys [current-state msg-payload]}]
                                   {:new-state (update-in current-state [:sum] + (:n msg-payload))})}
   :state-fn          (fn [_put-fn] {:state state-atom})})

(defn observing-cmp-map
  "Map for component that observes the state of another component."
  [cmp-id observed-atom]
  {:cmp-id           cmp-id
   :state-fn         (fn [_put-fn] {:state (atom {}) :observed observed-atom})
   :all-msgs-handler (fn [{:keys [msg]}] {:emit-msg msg})})


;; Tests for components observing the state of another component, as facilitated by :cmd/observe-state.

(deftest observe-1-1-test
  "1 observed component, 1 observing component. Test that observing component receives last published state
  of the observed component."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        observed-atom (atom {})]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(observed-cmp-map :test/recv-cmp-1 recv-state-1)
                         (observing-cmp-map :test/obs-cmp-1 observed-atom)}]
       [:cmd/observe-state {:from :test/recv-cmp-1 :to :test/obs-cmp-1}]])
    (doseq [n (range 1 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/recv-cmp-1 :msg [:test/sum {:n n}]}]))
    (testing ":test/recv-cmp-1 in desired state"
      (let [test-pred #(= (:sum %) 5050)]
        (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
        (is (test-pred @recv-state-1))))
    (testing "observed state atom mirrors original"
      (let [test-pred #(= @observed-atom @recv-state-1)]
        (tp/w-timeout 1000 (go (while (not (test-pred)))))
        (is (test-pred))
        (is (= (:sum @observed-atom) 5050))))))


(deftest observe-1-3-test
  "1 observed component, 3 observing component. Test that all receive the observed component's last
  published state."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        observed-atom-1 (atom {})
        observed-atom-2 (atom {})
        observed-atom-3 (atom {})]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(observed-cmp-map :test/recv-cmp-1 recv-state-1)
                         (observing-cmp-map :test/obs-cmp-1 observed-atom-1)
                         (observing-cmp-map :test/obs-cmp-2 observed-atom-2)
                         (observing-cmp-map :test/obs-cmp-3 observed-atom-3)}]
       [:cmd/observe-state {:from :test/recv-cmp-1 :to #{:test/obs-cmp-1 :test/obs-cmp-2 :test/obs-cmp-3}}]])
    (doseq [n (range 1 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/recv-cmp-1 :msg [:test/sum {:n n}]}]))
    (testing ":test/recv-cmp-1 in desired state"
      (let [test-pred #(= (:sum %) 5050)]
        (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
        (is (test-pred @recv-state-1))))
    (testing "observed state atoms mirror original"
      (let [test-pred #(= @observed-atom-1 @observed-atom-2 @observed-atom-3 @recv-state-1)]
        (tp/w-timeout 1000 (go (while (not (test-pred)))))
        (is (test-pred))
        (is (= (:sum @observed-atom-1) 5050))
        (is (= (:sum @observed-atom-2) 5050))
        (is (= (:sum @observed-atom-3) 5050))))))


(deftest observe-1-1-observed-xform-test
  "1 observed component, 1 observing component. Test that observing component applies xform function when
  receiving last published state of the observed component. Let's say we have an observing component
  with the habit of exaggerating the :sum by a factor of 2. That's something we can express as an xform
  function. Then we can test it was actually applied to the snapshot."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        observed-atom (atom {})
        observed-xform (fn [snapshot] (update-in snapshot [:sum] * 2))]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(observed-cmp-map :test/recv-cmp-1 recv-state-1)
                         (merge (observing-cmp-map :test/obs-cmp-1 observed-atom) {:observed-xform observed-xform})}]
       [:cmd/observe-state {:from :test/recv-cmp-1 :to :test/obs-cmp-1}]])
    (doseq [n (range 1 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/recv-cmp-1 :msg [:test/sum {:n n}]}]))
    (testing ":test/recv-cmp-1 in desired state"
      (let [test-pred #(= (:sum %) 5050)]
        (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
        (is (test-pred @recv-state-1))))
    (testing "observed state atom mirrors original"
      (let [test-pred #(= (:sum @observed-atom) 10100)]
        (tp/w-timeout 1000 (go (while (not (test-pred)))))
        (is (test-pred))))))


(deftest observe-1-1-snapshot-xform-test
  "1 observed component, 1 observing component. Test that observed component applies snapshot xform before
  publishing state snapshot. This can for example be useful in cases where the observed component stores
  secrets or anything else that should not be exposed. Here, let's dissoc a key."
  (let [switchboard (sb/component :test/my-switchboard)
        recv-state-1 (atom {:sum 0 :all-msg-handler-sum 0 :unhandled-handler-sum 0})
        observed-atom (atom {})
        snapshot-xform-fn #(dissoc % :all-msg-handler-sum)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp #{(merge (observed-cmp-map :test/recv-cmp-1 recv-state-1) {:snapshot-xform-fn snapshot-xform-fn})
                         (observing-cmp-map :test/obs-cmp-1 observed-atom)}]
       [:cmd/observe-state {:from :test/recv-cmp-1 :to :test/obs-cmp-1}]])
    (doseq [n (range 1 101)]
      (sb/send-cmd switchboard [:cmd/send {:to :test/recv-cmp-1 :msg [:test/sum {:n n}]}]))
    (testing ":test/recv-cmp-1 in desired state"
      (let [test-pred #(= (:sum %) 5050)]
        (tp/w-timeout 1000 (go (while (not (test-pred @recv-state-1)))))
        (is (test-pred @recv-state-1))))
    (testing "observed state atom mirrors original"
      (let [test-pred #(= @observed-atom {:sum 5050 :unhandled-handler-sum 0})]
        (tp/w-timeout 1000 (go (while (not (test-pred)))))
        (is (test-pred))
        (is (nil? (:all-msg-handler-sum @observed-atom)))))))
