(ns example.pointer
  (:gen-class)
  (:require
    [clojure.core.match :refer [match]]
    [incanter.distributions :as dist]
    [incanter.stats :as stats]
    [matthiasn.systems-toolbox.component :as comp]))

(defn mk-state [_] (atom {:count 0}))

(defn process-mouse-pos
  "Handler function for received mouse positions, increments counter and returns mouse position to sender."
  [app msg put-fn]
  (let [[_ params] msg
        d1 (Math/round (dist/draw (stats/sample-normal 1000 :mean 15 :sd 6)))
        d2 (Math/round (dist/draw (stats/sample-normal 1000 :mean 150 :sd 1)))]
    (swap! app update-in [:count] inc)
    #_(put-fn [:cmd/schedule-new {:timeout (if (pos? (dist/draw [0 0 0 0 0 1])) (+ d1 d2) d1)
                                :message (with-meta [:cmd/mouse-pos (assoc params :count (:count @app))] (meta msg))}])
    (put-fn (with-meta [:cmd/mouse-pos (assoc params :count (:count @app))] (meta msg)))
    ))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:cmd/mouse-pos _] (process-mouse-pos app msg put-fn)
         :else (println "Unmatched event in :pointer-cmp: " msg)))

(defn component [cmp-id] (comp/make-component cmp-id mk-state in-handler nil))