(ns example.ui-mouse-moves
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.helpers :refer [by-id]]))

;; some SVG defaults
(def circle-defaults {:fill "rgba(255,0,0,0.1)" :stroke "black" :stroke-width 2 :r 15})
(def text-default {:stroke "none" :fill "black" :style {:font-size 12}})
(def text-bold (merge text-default {:style {:font-weight :bold :font-size 12}}))

(defn circle-pos
  "Update circle position to account for page scroll position, as the SVG has a fixed position."
  [pos]
  (update-in pos [:y] - (.-pageYOffset js/window)))

(defn trailing-circles
  "Displays two transparent circles. The position of the circles comes from the most recent messages,
  one sent locally and the other with a roundtrip to the server in between.
  This makes it easier to visually experience any delays."
  [state]
  (let [local-pos (circle-pos (:local state))
        from-server (circle-pos (:from-server state))]
    [:g
     [:circle (merge circle-defaults {:cx (:x local-pos) :cy (:y local-pos)})]
     [:circle (merge circle-defaults {:cx (:x from-server) :cy (:y from-server) :fill "rgba(0,0,255,0.1)"})]]))

(defn text-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [state mean mn mx last-rt]
  [:g
   [:text (merge text-bold {:x 30 :y 20}) "Mouse Moves Processed:"]
   [:text (merge text-default {:x 183 :y 20}) (:count state)]
   [:text (merge text-bold {:x 30 :y 40}) "Processed since Startup:"]
   [:text (merge text-default {:x 183 :y 40}) (:count (:from-server state))]
   [:text (merge text-bold {:x 30 :y 60}) "Current Position:"]
   (when-let [pos (:local state)]
     [:text (merge text-default {:x 137 :y 60}) (str "x: " (:x pos) " y: " (:y pos))])
   [:text (merge text-bold {:x 30 :y 80}) "Latency (ms):"]
   (when last-rt
     [:text (merge text-default {:x 115 :y 80}) (str mean " mean / " mn " min / " mx " max / " last-rt " last")])])

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
    [:div
     [:svg {:width  (:width @local)
            :height (:width @local)}
      (text-view state-snapshot (.toFixed mean 0) mn mx last-rt)
      (trailing-circles state-snapshot)]]))

(defn init-fn
  "Listen to onmousemove events for entire page, emit message when fired."
  [{:keys [put-fn]}]
  (aset js/window "onmousemove"
        #(put-fn [:cmd/mouse-pos {:x (.-pageX %) :y (.-pageY %)}])))

(defn cmp-map
  "Configuration map for systems-toolbox-ui component."
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn mouse-view
              :dom-id  "mouse"
              :init-fn init-fn
              :cfg     {:msgs-on-firehose true}}))
