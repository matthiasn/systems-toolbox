(ns matthiasn.systems-toolbox.switchboard.init
  (:require [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.switchboard.spec]
    #?(:clj
            [clojure.core.async :refer [put! tap untap-all untap unsub-all close!]]
       :cljs [cljs.core.async :refer [put! tap untap-all untap unsub-all close!]])
    #?(:clj
            [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])
    #?(:clj
            [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])))

(defn cmp-maps-set
  "Returns a set with component maps."
  [val]
  (cond
    (set? val) val

    (vector? val)
    (do (l/warn "Use of vector is deprecated, use a set instead:" val)
        (set val))

    :else #{val}))

(defn wire-or-init-comp
  "Either wire existing and already instantiated component or instantiate a
   component from a component map.
   Also capable of reloading component, e.g. when using Figwheel on the client
   side.
   When a previous component with the same name exists, this function first of
   all unwires that previous component by unsubscribing and untapping all
   connected channels. Then, the state of that previous component is used in the
   new component in order to provide a smooth developer experience.
   Finally, the new component is tapped into the switchboard's firehose and the
   component is also asked to publish its state once (also useful for Figwheel)."
  [init?]
  (fn
    [{:keys [current-state msg-payload cmp-id]}]
    (let [cmp-maps-set (set (filter identity (cmp-maps-set msg-payload)))
          reducer-fn
          (fn [acc cmp]
            (let [cmp-id-to-wire (:cmp-id cmp)
                  firehose-chan (:firehose-chan (cmp-id (:components current-state)))
                  reload? (:reload-cmp (merge comp/component-defaults (:opts cmp)))
                  prev-cmp (get-in current-state [:components cmp-id-to-wire])]
              (when prev-cmp
                (untap-all (:firehose-mult prev-cmp))
                (untap (:firehose-mult (cmp-id (:components current-state)))
                       (:in-chan prev-cmp))
                (unsub-all (:out-pub prev-cmp))
                (unsub-all (:state-pub prev-cmp)))

              (when (and prev-cmp reload?)
                (when-let [shutdown-fn (:shutdown-fn prev-cmp)]
                  (shutdown-fn)))
              (let [cmp (if (or (not prev-cmp) reload?)
                          (if init? (comp/make-component cmp) cmp)
                          prev-cmp)]
                (if cmp
                  (let [in-chan (:in-chan cmp)
                        new-state (-> acc
                                      (assoc-in [:components cmp-id-to-wire] cmp)
                                      (update-in [:fh-taps] conj
                                                 {:from cmp-id-to-wire
                                                  :to   cmp-id
                                                  :type :fh-tap}))]
                    (when-let [prev-state (:watch-state prev-cmp)]
                      (reset! (:watch-state cmp) @prev-state))
                    (tap (:firehose-mult cmp) firehose-chan)
                    (let [known-cmp-ids (set (keys (:components new-state)))]
                      (s/def :st.switchboard/cmp known-cmp-ids))
                    (put! in-chan [:cmd/publish-state])
                    new-state)
                  acc))))
          new-state (reduce reducer-fn current-state cmp-maps-set)]
      {:new-state new-state})))

(defn shutdown-all
  "Call shutdown function on each component to prepare for reload."
  [{:keys [current-state]}]
  (let [cmps (:components current-state)]
    (doseq [cmp (vals cmps)]
      (when-let [shutdown-fn (:shutdown-fn cmp)]
        (shutdown-fn)))))

(defn shutdown-cmp
  "Call shutdown function on specified component."
  [{:keys [current-state msg-payload]}]
  (let [cmp (-> current-state :components msg-payload)
        new-state (update-in current-state [:components] dissoc msg-payload)]
    (when-let [shutdown-fn (:shutdown-fn cmp)]
      (shutdown-fn))
    {:new-state new-state
     :emit-msg  [:switchboard/status {:cmd    :shutdown
                                      :status :success}]}))
