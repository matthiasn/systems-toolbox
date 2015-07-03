(ns example.conf)

(def observer-cfg-map
  {:dom-id "observer"
   :switchbrd-id :client/switchboard
   :fixed-nodes   {:client/ws-cmp           {:x 100 :y 500}
                   :client/observer-cmp     {:x 500 :y 900}
                   :client/log-cmp          {:x 200 :y 900}
                   :client/switchboard      {:x 360 :y 680}
                   :client/mouse-cmp        {:x 650 :y 500}
                   :client/histogram-cmp    {:x 650 :y 700}
                   :client/jvmstats-cmp     {:x 100 :y 700}
                   :client/store-cmp        {:x 360 :y 320}}})
