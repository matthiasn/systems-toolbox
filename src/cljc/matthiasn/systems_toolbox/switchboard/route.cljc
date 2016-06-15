(ns matthiasn.systems-toolbox.switchboard.route
  (:require  [matthiasn.systems-toolbox.spec :as spec]
             [matthiasn.systems-toolbox.switchboard.helpers :as h]
    #?(:clj  [clojure.core.async :refer [chan pipe sub tap]]
       :cljs [cljs.core.async :refer [chan pipe sub tap]])
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])
             [clojure.set :as set]))

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
  [{:keys [current-state msg-payload]}]
  {:pre (empty? (set/intersection (h/cmp-ids-set (:from msg-payload)) (h/cmp-ids-set (:to msg-payload))))}
  (let [{:keys [from to only pred]} msg-payload
        connections (h/cartesian-product (h/cmp-ids-set from) (h/cmp-ids-set to))
        subscribe-reducer-fn (fn [acc [from to]]
                               (let [handled-messages (keys (:handler-map (to (:components acc))))
                                     msg-types (if only (flatten [only]) (vec handled-messages))
                                     subscribe (subscribe-fn from to pred)]
                                 (reduce subscribe acc msg-types)))]
    {:new-state (reduce subscribe-reducer-fn current-state connections)}))

;; TODO: implement filtering with comparable semantics as in route-handler, see issue #34
(defn route-all-handler
  "Connects two components where ALL messages are routed to recipient(s), not only those
  for which there is a specific handler. This results in both the all-msgs-handler and
  the unhandled-handler"
  [{:keys [current-state msg-payload]}]
  {:pre (empty? (set/intersection (h/cmp-ids-set (:from msg-payload)) (h/cmp-ids-set (:to msg-payload))))}
  (let [{:keys [from to pred]} msg-payload
        components (:components current-state)
        connections (h/cartesian-product (h/cmp-ids-set from) (h/cmp-ids-set to))
        reducer-fn (fn [acc [from to]]
                     (let [in-chan (:in-chan (to components))
                           target-chan (if pred
                                         (let [filtered-chan (chan 1 (filter pred))]
                                           (pipe filtered-chan in-chan)
                                           filtered-chan)
                                         in-chan)]
                       (tap (:out-mult (from components)) target-chan)
                       (update-in acc [:taps] conj {:from from :to to :type :tap})))]
    {:new-state (reduce reducer-fn current-state connections)}))
