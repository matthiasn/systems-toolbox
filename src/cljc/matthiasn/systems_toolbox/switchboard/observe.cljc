(ns matthiasn.systems-toolbox.switchboard.observe
  (:require
    #?(:clj  [clojure.core.async :refer [chan pipe sub]]
       :cljs [cljs.core.async :refer [chan pipe sub]])
    #?(:clj  [clojure.pprint :as pp]
       :cljs [cljs.pprint :as pp])))

(defn subscribe
  "Subscribe component to a specified publisher."
  [{:keys [cmp-state from to msg-type pred]}]
  (let [app @cmp-state
        [from-cmp from-pub] from
        [to-cmp to-chan] to
        pub-comp (from-cmp (:components app))
        sub-comp (to-cmp (:components app))
        target-chan (if pred
                      (let [filtered-chan (chan 1 (filter pred))]
                        (pipe filtered-chan (to-chan sub-comp))
                        filtered-chan)
                      (to-chan sub-comp))]
    (sub (from-pub pub-comp) msg-type target-chan)
    (swap! cmp-state update-in [:subs] conj {:from from-cmp :to to-cmp :msg-type msg-type :type :sub})))

(defn subscribe-comp-state
  "Subscribe component to a specified publisher."
  [{:keys [cmp-state put-fn from to]}]
  (doseq [t (flatten [to])]
    (subscribe {:cmp-state cmp-state
                :put-fn    put-fn
                :from      [from :state-pub]
                :msg-type  :app/state
                :to        [t :sliding-in-chan]})))

(defn observe-state
  [{:keys [cmp-state put-fn msg-payload]}]
  (let [{:keys [from to]} msg-payload]
    (subscribe-comp-state {:cmp-state cmp-state
                           :put-fn    put-fn
                           :from from
                           :to to})))
