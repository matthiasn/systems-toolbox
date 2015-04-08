(ns example.ui-mouse-svg
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]
            [cljs.core.match :refer-macros [match]]))

(def circle-defaults {:fill "rgba(255,0,0,0.1)" :stroke "black" :stroke-width 2 :r 15})
(def text-default {:stroke "none" :fill "black" :dy ".35em"})
(def text-bold (merge text-default {:style {:font-weight :bold}}))

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

(defn histogram-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [rtt-times]
  (let [freq (frequencies rtt-times)]
    [:g
     (for [[x y] freq] ^{:key (str x y)} [:rect {:x x :y (- 370 y) :fill "steelblue" :width 1 :height y}])
     (for [x (range 50 1000 50)]
       ^{:key (str "lines-x" x)} [:rect {:x x :y 372 :fill "black" :width 1 :height (if (= (mod x 100) 0) 10 5)}])
     [:rect {:x 0 :y 371 :fill "black" :width 1000 :height 1}]
     (for [x (range 100 1000 100)]
       ^{:key (str "label-x" x)} [:text (merge text-default {:x x :y 391 :text-anchor :middle}) x])]))

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
   [:text (merge text-bold {:y 12 :x 10}) "Mouse Moves Processed:"]
   [:text (merge text-default {:y 12 :x 215}) (:count state)]
   [:text (merge text-bold {:y 12 :x 265}) "Current Position:"]
   (when pos [:text (merge text-default {:y 12 :x 405}) (str "x: " (:x pos) " y: " (:y pos))])
   [:text (merge text-bold {:y 12 :x 530}) "Latency (ms):"]
   (when latency [:text (merge text-default {:y 12 :x 640}) (str (.toFixed mean 0) "/" mn "/" mx "/" latency)])
   [:text (merge text-default {:y 12 :x 840}) "(mean/min/max/last)"]])

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
    [:svg {:width chart-w
           :height chart-h
           :style {:background-color :white}
           :on-mouse-move (mouse-move-ev-handler app put-fn (r/current-component))}
     [text-view state pos mean mn mx latency]
     [trailing-circles state]
     [histogram-view rtt-times]]))

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