(ns example.ui-mouse-moves
  (:require [reagent.core :as rc]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.helpers :refer [by-id]]))

(def circle-defaults {:fill "rgba(255,0,0,0.1)" :stroke "black" :stroke-width 2 :r 15})
(def text-default {:stroke "none" :fill "black" :style {:font-size 12}})
(def text-bold (merge text-default {:style {:font-weight :bold :font-size 12}}))

(defn trailing-circles
  "Displays two transparent circles where one is drawn directly on the client and the other is drawn after a roundtrip.
  This makes it easier to experience any delays."
  [state local-state]
  (let [pos (:pos local-state)
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
     [:text (merge text-default {:x 115 :y 80}) (str mean " mean / " mn " min / " mx " max / " last-rt " last")])])

(defn mouse-touch-ev-handler
  "Handler function for mouse move and touch move events. Determines position from event and sends
  coordinates as :cmd/mouse-pos messages. Also sets position in the local atom."
  [local put-fn curr-cmp touch?]
  (fn [ev]
    (let [rect (-> curr-cmp rc/dom-node .getBoundingClientRect)
          ev (if touch? (aget (.-targetTouches ev) 0) ev)
          pos {:x (.toFixed (- (.-clientX ev) (.-left rect))) :y (.toFixed (- (.-clientY ev) (.-top rect)) 0)}]
      (swap! local assoc :pos pos)
      (put-fn [:cmd/mouse-pos pos])
      (.stopPropagation ev))))

(defn mouse-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [{:keys [observed local put-fn]}]
  (let [state-snapshot @observed
        mouse-div (by-id "mouse")
        last-rt (:rt-time (:from-server state-snapshot))
        rtt-times (:rtt-times state-snapshot)
        mx (apply max rtt-times)
        mn (apply min rtt-times)
        mean (/ (apply + rtt-times) (count rtt-times))
        update-width #(swap! local assoc :width (- (.-offsetWidth mouse-div) 2))]
    (update-width)
    (aset js/window "onresize" update-width)
    [:div {:style {:border-color :darkgray :border-width "1px" :border-style :solid}}
     [:svg {:width         (:width @local) :height (:width @local) ;220
            :style         {:background-color :white}
            :on-mouse-move (mouse-touch-ev-handler local put-fn (rc/current-component) false)
            :on-touch-move (mouse-touch-ev-handler local put-fn (rc/current-component) true)}
      (text-view state-snapshot (:pos @local) (.toFixed mean 0) mn mx last-rt)
      (trailing-circles state-snapshot @local)]]))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn mouse-view
              :dom-id  "mouse"
              :cfg     {:msgs-on-firehose true}}))
