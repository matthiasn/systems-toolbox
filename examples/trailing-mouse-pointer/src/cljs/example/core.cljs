(ns example.core
  (:require [example.spec]
            [example.store :as store]
            [example.ui-histograms :as hist]
            [example.ui-info :as info]
            [example.re-frame :as ui]
            [example.observer :as observer]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [clojure.string :as s]))

(enable-console-print!)

(defonce switchboard (sb/component :client/switchboard))

(def OBSERVER
  (or (.-OBSERVER js/window)
      (s/includes? (aget js/window "location" "search") "OBSERVER=true")))

(defn make-observable [components]
  (if OBSERVER
    (let [mapper #(assoc-in % [:opts :msgs-on-firehose] true)]
      (prn "Attaching firehose")
      (set (mapv mapper components)))
    components))

(def sente-cfg {:relay-types #{:mouse/pos
                               :mouse/get-hist
                               :firehose/cmp-put
                               :firehose/cmp-recv
                               :firehose/cmp-publish-state
                               :firehose/cmp-recv-state}
                :msgs-on-firehose true})

(defn init! []
  (let [components #{(sente/cmp-map :client/ws-cmp sente-cfg)
                     (ui/cmp-map :client/ui-cmp)
                     (store/cmp-map :client/store-cmp)}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]

       [:cmd/route {:from :client/ui-cmp
                    :to   #{:client/store-cmp :client/ws-cmp}}]

       [:cmd/route {:from #{:client/store-cmp
                            :client/ws-cmp}
                    :to   :client/store-cmp}]

       [:cmd/observe-state {:from :client/store-cmp
                            :to   :client/ui-cmp}]

       (when OBSERVER
         [:cmd/attach-to-firehose :client/ws-cmp])]))
  (observer/init! switchboard))

(init!)
