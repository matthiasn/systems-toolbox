(ns matthiasn.systems-toolbox.switchboard.route
  (:require  [matthiasn.systems-toolbox.spec :as spec]
    #?(:clj  [clojure.core.async :refer [chan pipe sub tap]]
       :cljs [cljs.core.async :refer [chan pipe sub tap]])
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])
             [clojure.set :as set]))

(defn cartesian-product
  "All the ways to take one item from each sequence.
  Borrowed from: https://github.com/clojure/math.combinatorics/blob/master/src/main/clojure/clojure/math/combinatorics.clj
  Reason: https://groups.google.com/forum/#!topic/clojure-dev/PDyOklDEv7Y"
  [& seqs]
  (let [v-original-seqs (vec seqs)
        step
        (fn step [v-seqs]
          (let [increment
                (fn [v-seqs]
                  (loop [i (dec (count v-seqs)), v-seqs v-seqs]
                    (if (= i -1) nil
                                 (if-let [rst (next (v-seqs i))]
                                   (assoc v-seqs i rst)
                                   (recur (dec i) (assoc v-seqs i (v-original-seqs i)))))))]
            (when v-seqs
              (cons (map first v-seqs)
                    (lazy-seq (step (increment v-seqs)))))))]
    (when (every? seq seqs)
      (lazy-seq (step v-original-seqs)))))

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

(defn cmp-ids-set
  "Returns a set with component IDs."
  [val]
  (cond (set? val) val
        (spec/namespaced-keyword? val) #{val}
        (vector? val) (do (l/warn "Use of vector is deprecated, use a set instead:" val)
                          (set val))))

(defn route-handler
  "Creates subscriptions between one component's out-pub and another component's in-chan.
  Requires a map with at least the :from and :to keys.
  Also, routing can be limited to message types specified under the :only keyword. Here, either
  a single message type or a vector with multiple message types can be used."
  [{:keys [current-state msg-payload]}]
  {:pre (empty? (set/intersection (cmp-ids-set (:from msg-payload)) (cmp-ids-set (:to msg-payload))))}
  (let [{:keys [from to only pred]} msg-payload
        connections (cartesian-product (cmp-ids-set from) (cmp-ids-set to))
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
  {:pre (empty? (set/intersection (cmp-ids-set (:from msg-payload)) (cmp-ids-set (:to msg-payload))))}
  (let [{:keys [from to pred]} msg-payload
        components (:components current-state)
        connections (cartesian-product (cmp-ids-set from) (cmp-ids-set to))
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
