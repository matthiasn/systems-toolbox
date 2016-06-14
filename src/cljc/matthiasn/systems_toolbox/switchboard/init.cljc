(ns matthiasn.systems-toolbox.switchboard.init
  (:require  [matthiasn.systems-toolbox.component :as comp]
             [matthiasn.systems-toolbox.switchboard.spec :as spec]
    #?(:clj  [clojure.core.async :refer [put! tap untap-all unsub-all]]
       :cljs [cljs.core.async :refer [put! tap untap-all unsub-all]])
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])
    #?(:clj  [clojure.spec :as s]
       :cljs [cljs.spec :as s])))

(defn wire-or-init-comp
  "Either wire existing and already instantiated component or instantiate a component from a component map.
  Also capable of reloading component, e.g. when using Figwheel on the client side.
  When a previous component with the same name exists, this function first of all unwires that previous
  component by unsubscribing and untapping all connected channels. Then, the state of that previous component is
  used in the new component in order to provide a smooth developer experience. When the either is no previous
  component with the same name or the component ought to be reloaded, the previous one is replaced by the new one in the
  switchboard state. Finally, the new component is tapped into the switchboard's firehose and the component is also
  asked to publish its state once (also useful for Figwheel)."
  [init?]
  (fn
    [{:keys [cmp-state msg-payload cmp-id]}]
    (let [switchbrd-snapshot @cmp-state]
      (doseq [cmp (flatten [msg-payload])]
        (let [cmp-id-to-wire (:cmp-id cmp)
              firehose-chan (:firehose-chan (cmp-id (:components @cmp-state)))
              reload? (:reload-cmp (merge comp/component-defaults (:opts cmp)))
              prev-cmp (get-in switchbrd-snapshot [:components cmp-id-to-wire])]
          (when (or (not prev-cmp) reload?)
            (when prev-cmp (untap-all (:firehose-mult prev-cmp))
                           (unsub-all (:out-pub prev-cmp))
                           (unsub-all (:state-pub prev-cmp))
                           (when-let [shutdown-fn (:shutdown-fn prev-cmp)]
                             (shutdown-fn)))
            (let [cmp (if init? (comp/make-component cmp) cmp)
                  in-chan (:in-chan cmp)]
              (when-let [prev-state (:watch-state prev-cmp)]
                (reset! (:watch-state cmp) @prev-state))
              (swap! cmp-state assoc-in [:components cmp-id-to-wire] cmp)
              (tap (:firehose-mult cmp) firehose-chan)
              (swap! cmp-state update-in [:fh-taps] conj {:from cmp-id-to-wire
                                                          :to   cmp-id
                                                          :type :fh-tap})
              (let [known-cmp-ids (set (keys (:components @cmp-state)))]
                (s/def :st.switchboard/cmp known-cmp-ids))
              (put! in-chan [:cmd/publish-state]))))))))
