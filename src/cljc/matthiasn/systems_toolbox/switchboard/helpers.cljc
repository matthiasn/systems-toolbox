(ns matthiasn.systems-toolbox.switchboard.helpers
  "Helper functions used by switchboard."
  (:require  [matthiasn.systems-toolbox.spec :as spec]
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])))

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

(defn cmp-ids-set
  "Returns a set with component IDs."
  [val]
  (cond (set? val) val
        (spec/namespaced-keyword? val) #{val}
        (vector? val) (do (l/warn "Use of vector is deprecated, use a set instead:" val)
                          (set val))))