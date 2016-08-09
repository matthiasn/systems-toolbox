(ns matthiasn.systems-toolbox.switchboard.observe
  (:require  [matthiasn.systems-toolbox.switchboard.helpers :as h]
    #?(:clj  [clojure.core.async :refer [sub]]
       :cljs [cljs.core.async :refer [sub]])))

(defn observe-state
  "Handler function for letting one component observe the state of another."
  [{:keys [current-state msg-payload]}]
  (let [{:keys [from to]} msg-payload
        reducer-fn
        (fn [acc to]
          (let [pub-comp (from (:components acc))
                sub-comp (to (:components acc))]
            (sub (:state-pub pub-comp) :app/state (:sliding-in-chan sub-comp))
            (update-in acc [:subs] conj {:from     from
                                         :to       to
                                         :msg-type :app/state
                                         :type     :sub})))]
    {:new-state (reduce reducer-fn current-state (h/cmp-ids-set to))}))
