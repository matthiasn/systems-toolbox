(ns matthiasn.systems-toolbox.component-test

  "Interact with components by sending some messages directly, see them handled correctly."

  (require [clojure.test :refer [deftest testing is]]
           [matthiasn.systems-toolbox.component :as component]
           [clojure.tools.logging :as log]))

(deftest cmp-all-msgs-handler
  "Tests that a very simple component that only has a handler for all messages regardless of type receives all
  messages sent to the component. State management of the component is not used here, instead we keep track
  of the messages in an atom that's external to the component and that the handler function has access to.
  A promise is used here which is delivered on when the message count received matches those sent. This does
  not tell us anything about the order yet, but it is still very useful when waiting for all messages to be
  delivered. In the subsequent assertion, we then check if the received messages are complete and in the
  expected order."
  (let [msgs-recvd (atom [])
        cnt 1000
        msgs-to-send (vec (range cnt))
        all-recvd (promise)
        cmp (component/make-component {:all-msgs-handler (fn [{:keys [msg-payload]}]
                                                           (swap! msgs-recvd conj msg-payload)
                                                           (when (= cnt (count @msgs-recvd))
                                                             (deliver all-recvd true)))})]
    (doseq [m msgs-to-send]
      (component/send-msg cmp [:some/type m]))

    (testing "all messages received: received count matches send count"
      (is (deref all-recvd 1000 false)))

    (testing "sent messages equal received messages"
      (is (= msgs-to-send @msgs-recvd)))))

(deftest cmp-all-msgs-handler-cmp-state
  "Like cmp-all-msgs-handler test, except that the handler function here acts on the component state provided
  in the map that the :all-msgs-handler function is called with.

  The number of messages sent in this test is somewhat arbitrarily, except that I wanted a) more than the 1024 pending
  puts that core.async allows and b) have confidence that a larger number of messages can be processed in
  very short time. Here, I test that more than 1K messages are processed per second. While timing can be problematic
  in tests, my laptop processes around 20K messages/s. If the rate dropped below 1K on ANY machine that is capable of
  running Java 8, that would indeed be a problem IMHO. However, this ought to quickly make visible any changes in the
  library that have an unfavorable complexity. Happy to discuss this approach further."
  (let [start-ts (component/now)
        msgs-recvd (atom [])
        cnt 10000
        msgs-to-send (vec (range cnt))
        all-recvd (promise)
        cmp (component/make-component {:state-fn         (fn [_put-fn] {:state msgs-recvd})
                                       :all-msgs-handler (fn [{:keys [msg-payload cmp-state]}]
                                                           (swap! cmp-state conj msg-payload)
                                                           (when (= cnt (count @cmp-state))
                                                             (deliver all-recvd true)))})]
    (doseq [m msgs-to-send]
      (component/send-msg cmp [:some/type m]))

    (testing "all messages received: received count matches send count"
      (is (deref all-recvd 1000 false)))

    (testing "processes more than 1K messages per second"
      (let [msgs-per-sec (int (* (/ 1000 (- (component/now) start-ts)) cnt))]
        (log/debug "Msgs/s:" msgs-per-sec)
        (is (> msgs-per-sec 1000))))

    (testing "sent messages equal received messages"
      (is (= msgs-to-send @msgs-recvd)))))

(deftest cmp-handlers-test
  "Tests that a) specific handlers receive only their respective messages, b) unhandled-handler receives only
  those that aren't handled specifically, and c) all-msgs-handler receives all.
  For this, I use integers once again, and partition the message types by using n mod 10 and n mod 100. For each number
  whose remainder is zero for one of these, I use a specific message type, and a generic one for all others. Then in
  the component state atom, it is easily verifiable that all items under a key match the expectations."
  (let [msgs-recvd (atom {:all        []
                          :div-by-10  []
                          :div-by-100 []
                          :unhandled  []})
        cnt 10000
        msgs-to-send (vec (range cnt))
        div-by-10? #(zero? (mod % 10))
        div-by-100? #(zero? (mod % 100))
        all-recvd (promise)
        all-msgs-handler (fn [{:keys [msg-payload cmp-state]}]
                           (swap! cmp-state update-in [:all] conj msg-payload)
                           (when (= cnt (count (:all @cmp-state)))
                             (deliver all-recvd true)))
        msg-handler (fn [k] (fn [{:keys [msg-payload cmp-state]}]
                              (swap! cmp-state update-in [k] conj msg-payload)))
        cmp (component/make-component {:state-fn          (fn [_put-fn] {:state msgs-recvd})
                                       :handler-map       {:int/div-by-10  (msg-handler :div-by-10)
                                                           :int/div-by-100 (msg-handler :div-by-100)}
                                       :unhandled-handler (msg-handler :unhandled)
                                       :all-msgs-handler  all-msgs-handler})]
    (doseq [m msgs-to-send]
      (let [msg-type (cond (div-by-100? m) :int/div-by-100
                           (div-by-10? m) :int/div-by-10
                           :else :some/type)]
        (component/send-msg cmp [msg-type m])))

    (testing "all messages received: received count matches send count"
      (is (deref all-recvd 1000 false)))

    (testing "sent messages equal received messages received by all-msg-handler"
      (is (= msgs-to-send (:all @msgs-recvd))))

    (testing "specific handlers only received their respective messages"
      (is (every? div-by-10? (:div-by-10 @msgs-recvd)))
      (is (every? div-by-100? (:div-by-100 @msgs-recvd))))

    (testing "unhandled handler did not receive any messages for which specific handler exists"
      (let [unhandled-items (:unhandled @msgs-recvd)]
        (is (every? #(not (div-by-10? %)) unhandled-items))
        (is (every? #(not (div-by-100? %)) unhandled-items))))))
