(ns example.conf)

(def observer-cfg-map
  {:dom-id "observer"
   :switchbrd-id :client/switchboard
   :fixed-nodes   {:client/ws-cmp           {:x 100 :y 300}
                   :client/observer-cmp     {:x 360 :y 550}
                   :client/switchboard      {:x 360 :y 420}
                   :client/mouse-cmp        {:x 650 :y 230}
                   :client/info-cmp         {:x 650 :y 360}
                   :client/histogram-cmp    {:x 650 :y 490}
                   :client/jvmstats-cmp     {:x 100 :y 440}
                   :client/store-cmp        {:x 360 :y 120}}
   :svg-props     {:viewBox "0 0 1000 600"}})
