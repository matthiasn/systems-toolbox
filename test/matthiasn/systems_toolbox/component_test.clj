(ns matthiasn.systems-toolbox.component-test

  "Interact with components by sending some messages directly, see them handled correctly."

  (require [clojure.test :refer [deftest testing is]]
           [matthiasn.systems-toolbox.component :as component]))

(deftest cmp-all-msgs-handler
  "Tests that a very simple component that only has a handler for all messages regardless of type receives all
  messages sent to the component. State management of the component is not used here, instead we keep track
  of the messages in an atom that's external to the component and that the handler function has access to.
  A promise is used here which is delivered on when the message count received matches those sent. This does
  not tell us anything about the order yet, but it is still very useful when waiting for all messages to be
  delivered. In the subsequent assertion, we then check if the received messages are complete and in the
  expected order."
  (let [msgs-recvd (atom [])
        cnt 10000
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
  in the map that the :all-msgs-handler function is called with."
  (let [msgs-recvd (atom [])
        cnt 10000
        msgs-to-send (vec (range cnt))
        all-recvd (promise)
        cmp (component/make-component {:state-fn         (fn [_put-fn] {:state msgs-recvd})
                                       :all-msgs-handler (fn [{:keys [msg-payload cmp-state]}]
                                                           (swap! cmp-state conj msg-payload)
                                                           (when (= cnt (count @msgs-recvd))
                                                             (deliver all-recvd true)))})]
    (doseq [m msgs-to-send]
      (component/send-msg cmp [:some/type m]))

    (testing "all messages received: received count matches send count"
      (is (deref all-recvd 1000 false)))
    (testing "sent messages equal received messages"
      (is (= msgs-to-send @msgs-recvd)))))
