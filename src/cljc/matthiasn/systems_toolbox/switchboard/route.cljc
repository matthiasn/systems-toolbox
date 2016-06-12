(ns matthiasn.systems-toolbox.switchboard.route
  (:require
    #?(:clj  [clojure.core.async :refer [chan pipe sub tap]]
       :cljs [cljs.core.async :refer [chan pipe sub tap]])
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])
    #?(:clj  [clojure.spec :as s]
       :cljs [cljs.spec :as s])))

(defn subscribe-fn
  "Subscribe component to a specified publisher."
  [from to pred]
  (fn [current-state msg-type]
    (let [in-chan (:in-chan (to (:components current-state)))
          target-chan (if pred (let [filtered-chan (chan 1 (filter pred))]
                                 (pipe filtered-chan in-chan)
                                 filtered-chan)
                               in-chan)]
      (sub (:out-pub (from (:components current-state))) msg-type target-chan)
      (update-in current-state [:subs] conj {:from from :to to :msg-type msg-type :type :sub}))))

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
            msg-types (if only (flatten [only]) (vec handled-messages))
            subscribe (subscribe-fn from to pred)]
        (doseq [msg-type msg-types]
          (reset! cmp-state (subscribe @cmp-state msg-type)))))))

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
