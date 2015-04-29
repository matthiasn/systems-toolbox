(ns example.ui-mouse-moves
  (:require [reagent.core :as rc]
            [matthiasn.systems-toolbox.reagent :as r]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]))

(def circle-defaults {:fill "rgba(255,0,0,0.1)" :stroke "black" :stroke-width 2 :r 15})
(def text-default {:stroke "none" :fill "black" :style {:font-size 12}})
(def text-bold (merge text-default {:style {:font-weight :bold :font-size 12}}))

(defn trailing-circles
  "Displays two transparent circles where one is drawn directly on the client and the other is drawn after a roundtrip.
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
  [state pos mean mn mx last-rt]
  [:g
   [:text (merge text-bold {:x 30 :y 20}) "Mouse Moves Processed:"]
   [:text (merge text-default {:x 183 :y 20}) (:count state)]
   [:text (merge text-bold {:x 30 :y 40}) "Processed since Startup:"]
   [:text (merge text-default {:x 183 :y 40}) (:count (:from-server state))]
   [:text (merge text-bold {:x 30 :y 60}) "Current Position:"]
   (when pos [:text (merge text-default {:x 137 :y 60}) (str "x: " (:x pos) " y: " (:y pos))])
   [:text (merge text-bold {:x 30 :y 80}) "Latency (ms):"]
   (when last-rt
     [:text (merge text-default {:x 115 :y 80})(str mean " mean / " mn " min / " mx " max / " last-rt " last")])])

(defn mouse-move-ev-handler
  "Handler function for mouse move events, triggered when mouse is moved above SVG. Sends coordinates to server."
  [app put-fn curr-cmp]
  (fn [ev]
    (let [rect (-> curr-cmp rc/dom-node .getBoundingClientRect)
          pos {:x (- (.-clientX ev) (.-left rect)) :y (.toFixed (- (.-clientY ev) (.-top rect)) 0)}]
      (put-fn [:cmd/mouse-pos-local pos])
      (put-fn [:cmd/mouse-pos pos])
      (.stopPropagation ev))))

(defn touch-move-ev-handler
  "Handler function for touch move events, triggered when finger is moved above SVG. Sends coordinates to server."
  [app put-fn curr-cmp]
  (fn [ev]
    (let [rect (-> curr-cmp rc/dom-node .getBoundingClientRect)
          t (aget (.-targetTouches ev) 0)
          pos {:x (- (.-clientX t) (.-left rect)) :y (.toFixed (- (.-clientY t) (.-top rect)) 0)}]
      (swap! app assoc :pos pos)
      (put-fn [:cmd/mouse-pos pos])
      (.stopPropagation ev))))

(defn mouse-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [app put-fn mouse-div]
  (let [state @app
        mouse-div (by-id "mouse")
        pos (:pos state)
        last-rt (:rt-time (:from-server state))
        rtt-times (:rtt-times state)
        mx (apply max rtt-times)
        mn (apply min rtt-times)
        mean (/ (apply + rtt-times) (count rtt-times))]
    [:div.pure-u-1 {:style {:border-color :darkgray :border-width "1px" :border-style :solid}}
     [:svg {:width         (- (.-offsetWidth mouse-div) 2) :height 200
            :style         {:background-color :white}
            :on-mouse-move (mouse-move-ev-handler app put-fn (rc/current-component))
            :on-touch-move (touch-move-ev-handler app put-fn (rc/current-component))}
      (text-view state pos (.toFixed mean 0) mn mx last-rt)
      (trailing-circles state)]]))

(defn component [cmp-id] (r/component cmp-id mouse-view "mouse" {}))