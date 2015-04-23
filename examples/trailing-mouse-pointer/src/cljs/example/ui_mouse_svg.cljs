(ns example.ui-mouse-svg
  (:require [reagent.core :as r :refer [atom]]
            [example.histogram :as hist]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]
            [cljs.core.match :refer-macros [match]]))

(def circle-defaults {:fill "rgba(255,0,0,0.1)" :stroke "black" :stroke-width 2 :r 15})
(def text-default {:stroke "none" :fill "black" :style {:font-size 12}})
(def text-bold (merge text-default {:style {:font-weight :bold :font-size 12}}))

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
  [state pos mean mn mx latency]
  [:g
   [:text (merge text-bold {:x 30 :y 20}) "Mouse Moves Processed:"]
   [:text (merge text-default {:x 183 :y 20}) (:count state)]
   [:text (merge text-bold {:x 30 :y 40}) "Current Position:"]
   (when pos [:text (merge text-default {:x 137 :y 40}) (str "x: " (:x pos) " y: " (:y pos))])
   [:text (merge text-bold {:x 30 :y 60}) "Latency (ms):"]
   (when latency
     [:text (merge text-default {:x 115 :y 60}) (str mean " mean / " mn " min / " mx " max / " latency " last")])])

(defn mouse-view
  "Renders SVG with an area in which mouse moves are detected. They are then sent to the server and the round-trip
  time is measured."
  [app put-fn mouse-div]
  (let [state @app
        pos (:pos state)
        latency (:latency (:from-server state))
        rtt-times (:rtt-times state)
        mx (apply max rtt-times)
        mn (apply min rtt-times)
        mean (/ (apply + rtt-times) (count rtt-times))]
    [:div.pure-u-1 {:style {:border-color :darkgray :border-width "1px" :border-style :solid}}
     [:svg {:width (- (.-offsetWidth mouse-div) 2) :height 200
            :style {:background-color :white}
            :on-mouse-move (mouse-move-ev-handler app put-fn (r/current-component))}
      [text-view state pos (.toFixed mean 0) mn mx latency]
      [trailing-circles state]]]))

(defn histogram-view
  "Renders histograms with roundtrip times."
  [app put-fn]
  (let [state @app
        rtt-times (:rtt-times state)]
    [:div.pure-g
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :style {:background-color :white} :viewBox "0 0 400 250"}
       [hist/histogram-view rtt-times 80 180 300 160 "Roundtrip t/ms"]]]
     [:div.pure-u-1.pure-u-sm-1-2.pure-u-md-1-3
      [:svg {:width "100%" :style {:background-color :white} :viewBox "0 0 400 250"}
       [hist/histogram-view (hist/percentile-range rtt-times 99) 80 180 300 160
        "Roundtrip t/ms (within 99th percentile)"]]]]))

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
  (let [app (atom {:count 0 :rtt-times []})
        mouse-div (by-id "mouse")]
    (r/render-component [mouse-view app put-fn mouse-div] mouse-div)
    (r/render-component [histogram-view app put-fn] (by-id "histograms"))
    app))

(defn component
  [cmp-id]
  (comp/make-component cmp-id mk-state in-handler nil))