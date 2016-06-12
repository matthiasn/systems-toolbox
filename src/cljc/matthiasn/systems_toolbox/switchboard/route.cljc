(ns matthiasn.systems-toolbox.switchboard.route
  (:require
    #?(:clj  [clojure.core.async :refer [chan pipe sub tap]]
       :cljs [cljs.core.async :refer [chan pipe sub tap]])
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])
    #?(:clj  [clojure.spec :as s]
       :cljs [cljs.spec :as s])))

(defn subscribe
  "Subscribe component to a specified publisher."
  [{:keys [cmp-state from to msg-type pred]}]
  (let [app @cmp-state
        pub-comp (from (:components app))
        sub-comp (to (:components app))
        in-chan (:in-chan sub-comp)
        target-chan (if pred (let [filtered-chan (chan 1 (filter pred))]
                               (pipe filtered-chan in-chan)
                               filtered-chan)
                             in-chan)]
    (sub (:out-pub pub-comp) msg-type target-chan)
    (swap! cmp-state update-in [:subs] conj {:from from :to to :msg-type msg-type :type :sub})))

(defn route-handler
  "Creates subscriptions between one component's out-pub and another component's in-chan.
  Requires a map with at least the :from and :to keys.
  Also, routing can be limited to message types specified under the :only keyword. Here, either
  a single message type or a vector with multiple message types can be used."
  [{:keys [cmp-state msg-payload]}]
  (let [{:keys [from to only pred]} msg-payload
        from-set (set (flatten [from]))]
    (doseq [from from-set]
      (let [handled-messages (keys (:handler-map (to (:components @cmp-state))))

            ;; TODO: only should be intersection of handlers and only items
            msg-types (if only (flatten [only]) (vec handled-messages))]
        (doseq [msg-type msg-types]
          (subscribe {:cmp-state cmp-state
                       :from      from
                       :to        to
                       :msg-type  msg-type
                       :pred      pred}))))))

;; TODO: implement filtering with comparable semantics as in route-handler, see issue #34
(defn route-all-handler
  [{:keys [current-state msg-payload]}]
  (let [{:keys [from to pred]} msg-payload]
    (doseq [from (flatten [from])]
      (let [components (:components current-state)
            mult-comp (from components)
            tap-comp (to components)
            error-log #(l/error "Could not create tap: " from " -> " to " - " %)
            target-chan (if pred
                          (let [filtered-chan (chan 1 (filter pred))]
                            (pipe filtered-chan (:in-chan tap-comp))
                            filtered-chan)
                          (:in-chan tap-comp))]
        (try (do
               (tap (:out-mult mult-comp) target-chan)
               {:new-state (update-in current-state [:taps] conj {:from from :to to :type :tap})})
             #?(:clj (catch Exception e (error-log (.getMessage e)))
                :cljs (catch js/Object e (error-log e))))))))
