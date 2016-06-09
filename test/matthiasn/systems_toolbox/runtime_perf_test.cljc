(ns matthiasn.systems-toolbox.runtime-perf-test

  "This namespace provides a sanity check before endeavoring in premature optimization. For example,
  if we can swap or reset an atom 70 million times per second on the JVM but 'only' process 80K messages
  per second, it is quite unlikely that using volatile! instead of an atom would speed things up. Here,
  core.async seems to be the more interesting candidate to look at, where the very simple operation
  of putting a message on a chan with an attached mult and no other chan to take it off there can be
  performed a little over 200K times per second."

  #?(:cljs (:require-macros [cljs.core.async.macros :refer [go go-loop]]))
  (:require
    #?(:clj  [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
    #?(:clj  [clojure.core.async :refer [<! chan mult buffer put! go go-loop timeout promise-chan >! tap
                                         sliding-buffer onto-chan]]
       :cljs [cljs.core.async :refer [<! chan mult put! timeout promise-chan >! tap sliding-buffer]])
             [matthiasn.systems-toolbox.test-promise :as tp]
             [matthiasn.systems-toolbox.component :as component]
    #?(:clj  [clojure.tools.logging :as log]
       :cljs [matthiasn.systems-toolbox.log :as log])))

; here, we can tweak the number of test runs, which is useful if we are interested in JIT optimizations
(def test-runs 1)

(defn swap-atom-repeatedly-fn
  []
  "This test aims at getting some perspective how expensive swapping an atom is in Clojure/ClojureScript.
  Answer: not terribly expensive. On the JVM, this can be performed around 70 million times per second,
  whereas in ClojureScript, this can be done 15 million times per second (2015 Retina MacBook)."
  (let [start-ts (component/now)
        cnt (* 1000 1000)
        state (atom 0)]
    (dotimes [_ cnt] (swap! state inc))
    (let [ops-per-sec (int (* (/ 1000 (- (component/now) start-ts)) cnt))]
      (log/debug "Atom swaps/s:" ops-per-sec)
      (is (> ops-per-sec 1000)))))

(deftest swap-atom-repeatedly
  (dotimes [_ test-runs]
    (swap-atom-repeatedly-fn)))

(defn swap-watched-atom-repeatedly-fn
  []
  "This test aims at getting some perspective how expensive swapping an atom is in Clojure/ClojureScript.
  Answer: not terribly expensive. On the JVM, this can be performed around 70 million times per second,
  whereas in ClojureScript, this can be done 15 million times per second (2015 Retina MacBook)."
  (let [start-ts (component/now)
        cnt (* 1000 1000)
        state (atom 0)]
    (add-watch state :watcher (fn [_ _ _ _new-state] #()))
    (dotimes [_ cnt] (swap! state inc))
    (let [ops-per-sec (int (* (/ 1000 (- (component/now) start-ts)) cnt))]
      (log/debug "Watched atom swaps/s:" ops-per-sec)
      (is (> ops-per-sec 1000)))))

(deftest swap-watched-atom-repeatedly
  (dotimes [_ test-runs]
    (swap-watched-atom-repeatedly-fn)))

(defn reset-atom-repeatedly-fn
  []
  "This test aims at getting some perspective how expensive resetting an atom is in Clojure/ClojureScript.
  Answer: not terribly expensive. On the JVM, this can be performed around 90 million times per second,
  whereas in ClojureScript, this can be done 15 million times per second on PhantomJS and over 60 million
  times per second in Firefox (2015 Retina MacBook)."
  (let [start-ts (component/now)
        cnt (* 1000 1000)
        state (atom 0)]
    (dotimes [n cnt] (reset! state n))
    (let [ops-per-sec (int (* (/ 1000 (- (component/now) start-ts)) cnt))]
      (log/debug "Atom resets/s:" ops-per-sec)
      (is (> ops-per-sec 1000)))))

(deftest reset-atom-repeatedly
  (dotimes [_ test-runs]
    (reset-atom-repeatedly-fn)))

(defn deref-atom-repeatedly-fn
  []
  "This test aims at getting some perspective how expensive dereferencing an atom is in Clojure/ClojureScript.
  Answer: not terribly expensive. On the JVM, this can be performed around 30 million times per second,
  whereas in ClojureScript, this can be done 8 million times per second (2015 Retina MacBook)."
  (let [start-ts (component/now)
        cnt (* 1000 1000)
        state (atom {:foo 1000})]
    (dotimes [n cnt] (/ (:foo @state) 10))
    (let [ops-per-sec (int (* (/ 1000 (- (component/now) start-ts)) cnt))]
      (log/debug "Atom derefs/s:" ops-per-sec)
      (is (> ops-per-sec 1000)))))

(deftest deref-atom-repeatedly
  (dotimes [_ test-runs]
    (deref-atom-repeatedly-fn)))

(defn put-on-chan-repeatedly-fn
  "Channel with attached mult and no other channels tapping into mult: messages silently dropped."
  []
  (let [start-ts (component/now)
        cnt (* 100 1000)
        ch (chan)
        m (mult ch)
        done (promise-chan)]
    (go
      (dotimes [n cnt] (>! ch n))
      (put! done true))

    (tp/w-timeout cnt (go
                        (testing "all messages received"
                          (is (true? (<! done))))
                        (let [ops-per-sec (int (* (/ 1000 (- (component/now) start-ts)) cnt))]
                          (log/debug "Channel puts/s:" ops-per-sec)
                          (is (> ops-per-sec 1000)))))))

(deftest put-on-chan-repeatedly1 (put-on-chan-repeatedly-fn))
#_#_#_#_#_
(deftest put-on-chan-repeatedly2 (put-on-chan-repeatedly-fn))
(deftest put-on-chan-repeatedly3 (put-on-chan-repeatedly-fn))
(deftest put-on-chan-repeatedly4 (put-on-chan-repeatedly-fn))
(deftest put-on-chan-repeatedly5 (put-on-chan-repeatedly-fn))
(deftest put-on-chan-repeatedly6 (put-on-chan-repeatedly-fn))

(deftest put-consume-repeatedly
  "Channel with attached go-loop, simple calculation using messages from channel."
  (let [start-ts (component/now)
        cnt (* 100 1000)
        ch (chan)
        state (atom 0)
        done (promise-chan)]
    (go (dotimes [n cnt] (>! ch n)))

    (go-loop []
      (let [n (<! ch)
            res (+ @state n)]
        (reset! state res)
        (when (= (dec cnt) n)
          (put! done true)))
      (recur))

    (tp/w-timeout cnt (go
                        (testing "all messages received"
                          (is (true? (<! done))))
                        (let [ops-per-sec (int (* (/ 1000 (- (component/now) start-ts)) cnt))]
                          (log/debug "Channel puts and consume/s:" ops-per-sec)
                          (is (> ops-per-sec 1000)))
                        (testing "all messages received (sum of all number sent matches)"
                          (is (= @state (reduce + (range cnt)))))))))

(deftest put-consume-mult-repeatedly
  "Channel with attached go-loop, simple calculation using messages from channel."
  (let [start-ts (component/now)
        cnt (* 100 1000)
        ch (chan)
        m (mult ch)
        ch2 (chan)
        state (atom 0)
        done (promise-chan)]
    (go (dotimes [n cnt] (>! ch n)))

    (tap m ch2)

    (go-loop []
      (let [n (<! ch2)
            res (+ @state n)]
        (reset! state res)
        (when (= (dec cnt) n)
          (put! done true)))
      (recur))

    (tp/w-timeout cnt (go
                        (testing "all messages received"
                          (is (true? (<! done))))
                        (let [ops-per-sec (int (* (/ 1000 (- (component/now) start-ts)) cnt))]
                          (log/debug "Channel puts and consume from mult/s:" ops-per-sec)
                          (is (> ops-per-sec 1000)))
                        (testing "all messages received (sum of all number sent matches)"
                          (is (= @state (reduce + (range cnt)))))))))

(defn put-consume-mult-w-pub-repeatedly-fn
  "Channel with attached go-loop, simple calculation using messages from channel, publication of state change. This
  imitates the basic use case of the systems-toolbox: there's a go-loop, some processing and publication of component
  state. Running this test gives some perspective of the amount of overhead that the systems-toolbox introduces,
  such as adding metadata to messages."
  []
  (let [start-ts (component/now)
        cnt (* 100 1000)
        ch (chan)
        m (mult ch)
        ch2 (chan)
        state-pub-chan (chan (sliding-buffer 1))
        state-mult (mult state-pub-chan)
        state (atom 0)
        done (promise-chan)]

    (go-loop []
      (let [n (<! ch2)
            res (+ @state n)]
        (reset! state res)
        (>! state-pub-chan res)
        (when (= (dec cnt) n)
          (put! done true)))
      (recur))

    (tap m ch2)
    (go (dotimes [n cnt] (>! ch n)))

    (tp/w-timeout cnt (go
                        (testing "promise delivered"
                          (is (true? (<! done))))
                        (let [ops-per-sec (int (* (/ 1000 (- (component/now) start-ts)) cnt))]
                          (log/debug "Channel puts and consume from mult/s (w/pub):" ops-per-sec)
                          (is (> ops-per-sec 1000)))
                        (testing "all messages received (sum of all number sent matches)"
                          (is (= @state (reduce + (range cnt)))))
                        :done))))

(deftest put-consume-mult-w-pub-repeatedly (put-consume-mult-w-pub-repeatedly-fn))

#_#_#_#_#_
(deftest put-consume-mult-w-pub-repeatedly2 (put-consume-mult-w-pub-repeatedly-fn))
(deftest put-consume-mult-w-pub-repeatedly3 (put-consume-mult-w-pub-repeatedly-fn))
(deftest put-consume-mult-w-pub-repeatedly4 (put-consume-mult-w-pub-repeatedly-fn))
(deftest put-consume-mult-w-pub-repeatedly5 (put-consume-mult-w-pub-repeatedly-fn))
(deftest put-consume-mult-w-pub-repeatedly6 (put-consume-mult-w-pub-repeatedly-fn))
