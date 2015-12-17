(ns example.pointer
  (:gen-class)
  (:require
    [incanter.distributions :as dist]
    [incanter.stats :as stats]
    [matthiasn.inspect.probe :as p :refer [defn]]))

(p/init)

(defn ptr-state-fn
  "Creates fresh component state."
  [_]
  {:state (atom {:count 0})})

(defn process-mouse-pos
  "Handler function for received mouse positions, increments counter and returns mouse position to sender."
  [{:keys [cmp-state msg put-fn]}]
  (let [[msg-type params] msg
        d1 (Math/round (dist/draw (stats/sample-normal 1000 :mean 15 :sd 6)))
        d2 (Math/round (dist/draw (stats/sample-normal 1000 :mean 150 :sd 1)))]
    (swap! cmp-state update-in [:count] inc)
    #_(put-fn [:cmd/schedule-new {:timeout (if (pos? (dist/draw [0 0 0 0 0 1])) (+ d1 d2) d1)
                                :message (with-meta [:cmd/mouse-pos
                                                     (assoc params :count (:count @cmp-state))] (meta msg))}])
    (put-fn (with-meta [:cmd/mouse-pos (assoc params :count (:count @cmp-state))] (meta msg)))))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    ptr-state-fn
   :handler-map {:cmd/mouse-pos process-mouse-pos}})
