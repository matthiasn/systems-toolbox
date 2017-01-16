(ns matthiasn.systems-toolbox.handler-utils-test
  "Test that handler utils work as expected."
  (:require [matthiasn.systems-toolbox.test-spec]
            [matthiasn.systems-toolbox.handler-utils :as hu]
   #?(:clj  [clojure.test :refer [deftest testing is]]
      :cljs [cljs.test :refer-macros [deftest testing is]])))

(deftest assoc-in-cmp-test
  (testing "state map is updated as expected"
    (let [handler (hu/assoc-in-cmp [:test :a])]
      (is (= {:new-state {:test {:a 2}}}
             (handler {:current-state {:test {}}
                       :msg-payload 2}))))))

(deftest update-in-cmp-test
  (testing "state map is updated as expected"
    (let [handler (hu/update-in-cmp [:test :a] inc)]
      (is (= {:new-state {:test {:a 2}}}
             (handler {:current-state {:test {:a 1}}}))))))
