(ns matthiasn.systems-toolbox.ui.jvmstats
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [reagent.core :as r :refer [atom]]
            [cljs.core.match :refer-macros [match]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]
            [goog.string :as gstring]
            [goog.string.format]))

(defn bar
  "Renders a vertical bar."
  [x y h w]
  [:rect {:x x :y (- y h) :fill "steelblue" :width w :height h}])

(defn reading-view
  "View for displaying a JVM stats reading. In the case of the average system load, there's also a sparkline chart
  for showing how the average load develops over time.
  CAVEAT: this is not as useful as it could be. The JVM's system load average is an average over the last minute,
  hence the slow response of the value, and more visibly, the sparkline. For more timely values, one could for
  example call 'top' on the shell for the current PID at any useful interval and parse the text.
  However, there are likely platform-specific differences to take into account.
  TODO: refactoring, pulling out sparkline function, among other things."
  [app]
  (let [state @app
        fmt #(gstring/format %1 %2)
        floor #(Math/floor %)
        readings (:readings state)
        cpu-loads (map :system-load-avg readings)
        last-n (take-last 30 cpu-loads)
        indexed (vec (map-indexed vector last-n))
        latest (last readings)
        sys-load-avg (:system-load-avg latest)
        available-cpus (:available-cpus latest)
        uptime (:uptime latest)
        m 60000 s 1000           ; minutes and seconds in milliseconds for uptime calculation
        w 3 gap 1 sparkline-h 16 ; bar width, height, and gap between bars
        chart-w 292 chart-h 44]  ; chart dimensions
    [:div
     [:svg {:width chart-w :height chart-h :style {:background-color :white}}
      [:g
       [:text {:y 12 :x 10 :stroke "none" :fill "black" :dy ".35em" :style {:font-weight :bold}}
        "Sys Load Avg:"]
       [:text {:y 12 :x 128 :stroke "none" :fill "black" :dy ".35em"}
        (when latest (str (fmt "%.2f" (-> sys-load-avg (/ available-cpus) (* 100))) "%"))]
       (for [[idx v] indexed]
         ^{:key (str idx v)} [bar (+ (* idx w) 194) (+ 3 sparkline-h) (* (/ v available-cpus) sparkline-h) (- w gap)])
       [:text {:y 32 :x 10 :stroke "none" :fill "black" :dy ".35em" :style {:font-weight :bold}} "CPUs:"]
       [:text {:y 32 :x 64 :stroke "none" :fill "black" :dy ".35em"} available-cpus]
       [:text {:y 32 :x 80 :stroke "none" :fill "black" :dy ".35em" :style {:font-weight :bold}} "Uptime:"]
       [:text {:y 32 :x 144 :stroke "none" :fill "black" :dy ".35em"}
        (str (floor (/ uptime m)) "m" (floor (/ (rem uptime m) s)) "s")]]]]))

(defn mk-state
  "Return clean initial component state atom."
  [dom-id]
  (fn [put-fn]
    (let [app (atom {:readings []})]
      (r/render-component [reading-view app] (by-id dom-id))
      app)))

(defn recv-jvm-stats
  ""
  [app stats]
  (swap! app assoc :readings (conj (:readings @app) stats)))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:stats/jvm stats] (recv-jvm-stats app stats)
         :else (println "Unmatched event:" msg)))

(defn component
  [cmp-id dom-id]
  (comp/make-component cmp-id (mk-state dom-id) in-handler nil))
