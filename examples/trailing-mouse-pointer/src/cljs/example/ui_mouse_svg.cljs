(ns example.ui-mouse-svg
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]
            [cljs.core.match :refer-macros [match]]))

(def circle-defaults {:fill "rgba(255,0,0,0.1)" :stroke "black" :stroke-width 2 :r 15})
(def text-default {:stroke "none" :fill "black" :dy ".35em"})
(def text-bold (merge text-default {:style {:font-weight :bold}}))
(def axis-label (merge text-default {:font-size 12}))
(def x-axis-label (merge axis-label {:font-size 12 :text-anchor :middle}))

(defn now [] (.getTime (js/Date.)))

(defn mouse-move-ev-handler
  "Handler function for mouse move events, triggered when mouse is moved above SVG. Sends timestamped
  coordinates to server."
  [app put-fn curr-cmp]
  (fn [ev]
    (let [rect (-> curr-cmp r/dom-node .getBoundingClientRect)
          pos {:x (- (.-clientX ev) (.-left rect)) :y (.toFixed (- (.-clientY ev) (.-top rect)) 0) :timestamp (now)}]
      (swap! app assoc :pos pos)
      (put-fn [:cmd/mouse-pos pos])
      (.stopPropagation ev))))

(def path-defaults {:fill :black :stroke :black :stroke-width 1})

(defn tick-length
  "Determines length of tick for the chart axes."
  [n]
  (cond
    (zero? (mod n 100)) 9
    (zero? (mod n 50)) 6
    :else 3))

(defn x-axis
  "Draws x-axis of a chart."
  [x y l scale]
  [:g
   [:path (merge path-defaults {:d (str "M" x " " y "l" (* l scale) " 0 l 0 -4 l 10 4 l -10 4 l 0 -4 z")})]
   (for [n (range 0 l 10)]
     ^{:key (str "xt" n)} [:path (merge path-defaults {:d (str "M" (+ x (* n scale)) " " y "l 0 " (tick-length n))})])
   (for [n (range 0 l 50)]
     ^{:key (str "xl" n)} [:text (merge x-axis-label {:x (+ x (* n scale)) :y (+ y 17)}) n])])

(defn y-axis
  "Draws y-axis of a chart."
  [x y l scale]
  [:g
   [:path (merge path-defaults {:d (str "M" x " " y "l 0 " (* l scale -1) " l -4 0 l 4 -10 l 4 10 l -4 0 z")})]
   (for [n (range 0 l 10)]
     ^{:key (str "yt" n)} [:path (merge path-defaults
                                        {:d (str "M" x " " (- y (* n scale)) "l -" (tick-length n) " 0")})])
   (for [n (range 0 l 50)]
     ^{:key (str "yl" n)} [:text (merge axis-label {:x (- x 10) :y (- y (* n scale)) :text-anchor :end}) n])])

(defn histogram-view
  "Renders a histogram chart for roundtrip times in ms and their frequencies."
  [rtt-times x y max-v]
  (let [freq (frequencies rtt-times)
        max-freq (apply max (map (fn [[_ f]] f) freq))
        scale 2
        x-axis-l (+ (* (Math/ceil (/ max-v 50)) 50) 20)
        y-axis-l (min (+ (* (Math/ceil (/ max-freq 50)) 50) 20) 120)]
    (when-not (empty? freq)
      [:g
       (for [[v f] freq]
         ^{:key (str "b" v f)} [:rect {:x (+ x (* v scale)) :y (- y (* f scale))
                                       :fill "steelblue" :width 1.3 :height (* f scale)}])
       [x-axis x y x-axis-l scale]
       [:text (merge text-bold x-axis-label {:x (+ x x-axis-l -10) :y (+ y 35)}) "Roundtrip t/ms"]
       [:text (let [x-coord (- x 45) y-coord (- y y-axis-l 10) rotate (str "rotate(270 " x-coord " " y-coord ")")]
                (merge text-bold x-axis-label {:x x-coord :y y-coord :transform rotate}))
        "Frequencies"]
       [y-axis x y y-axis-l scale]])))

(defn trailing-circles
  "Displays two transparent circles where one is drawn directly on the client and the other is drawn after a rountrip.
  This makes it easier to experience any delays."
  [state]
  (let [pos (:pos state)
        from-server (:from-server state)]
    [:g
     [:circle (merge circle-defaults {:cx (:x pos) :cy (:y pos)})]
     [:circle (merge circle-defaults {:cx (:x from-server) :cy (:y from-server) :fill "rgba(0,0,255,0.1)"})]]))

(defn text-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [state pos mean mn mx latency]
  [:g
   [:text (merge text-bold {:x 10 :y 12}) "Mouse Moves Processed:"]
   [:text (merge text-default {:x 215 :y 12}) (:count state)]
   [:text (merge text-bold {:x 265 :y 12}) "Current Position:"]
   (when pos [:text (merge text-default {:x 405 :y 12}) (str "x: " (:x pos) " y: " (:y pos))])
   [:text (merge text-bold {:x 530 :y 12}) "Latency (ms):"]
   (when latency 
     [:text (merge text-default {:x 640 :y 12}) (str mean " mean / " mn " min / " mx " max / " latency " last")])])

(defn mouse-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [app put-fn]
  (let [state @app
        pos (:pos state)
        chart-w 1000 chart-h 400
        latency (:latency (:from-server state))
        rtt-times (:rtt-times state)
        mx (apply max rtt-times)
        mn (apply min rtt-times)
        mean (/ (apply + rtt-times) (count rtt-times))]
    [:svg {:width chart-w :height chart-h :style {:background-color :white}
           :on-mouse-move (mouse-move-ev-handler app put-fn (r/current-component))}
     [text-view state pos (.toFixed mean 0) mn mx latency]
     [trailing-circles state]
     [histogram-view rtt-times 80 340 mx]]))

(defn mouse-pos-from-server!
  "Handler function for mouse position messages received from server."
  [app pos]
  (let [latency (- (now) (:timestamp pos))
        with-ts (assoc pos :latency latency)]
    (swap! app assoc :from-server with-ts)
    (swap! app update-in [:count] inc)
    (swap! app update-in [:rtt-times] conj latency)))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:cmd/mouse-pos-proc pos] (mouse-pos-from-server! app pos)
         :else (prn "unknown msg in data-loop" msg)))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:count 0 :rtt-times []})]
    (r/render-component [mouse-view app put-fn] (by-id "mouse"))
    app))

(defn component
  [cmp-id]
  (comp/make-component cmp-id mk-state in-handler nil))