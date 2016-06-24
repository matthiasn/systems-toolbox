(ns example.ui-mouse-moves
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.helpers :refer [by-id]]))

;; some SVG defaults
(def circle-defaults {:fill "rgba(255,0,0,0.1)" :stroke "rgba(0,0,0,0.5)" :stroke-width 2 :r 15})
(def text-default {:stroke "none" :fill "black" :style {:font-size 12}})
(def text-bold (merge text-default {:style {:font-weight :bold :font-size 12}}))

(defn trailing-circles
  "Displays two transparent circles. The position of the circles comes from the most recent messages,
  one sent locally and the other with a roundtrip to the server in between.
  This makes it easier to visually experience any delays."
  [state]
  (let [local-pos (:local state)
        from-server (:from-server state)
        prev-local (map-indexed vector (:local-positions state))]
    [:g
     [:circle (merge circle-defaults {:cx (:x local-pos) :cy (:y local-pos)})]
     [:circle (merge circle-defaults {:cx (:x from-server) :cy (:y from-server) :fill "rgba(0,0,255,0.1)"})]
     (when (and (:show-all state) (seq prev-local))
       (for [[idx pos] prev-local]
         ^{:key (str "circle" idx)}
         [:circle {:stroke "rgba(0,0,0,0.05)"
                   :stroke-width 2 :r 15
                   :cx (:x pos) :cy (:y pos)
                   :fill "rgba(0,255,0,0.03)"}]))]))

(defn mouse-view
  "Renders SVG with both local mouse position and the last one returned from the server,
  in an area that covers the entire visible page."
  [{:keys [observed local]}]
  (let [state-snapshot @observed
        mouse-div (by-id "mouse")
        update-dim #(do (swap! local assoc :width (- (.-offsetWidth mouse-div) 2))
                        (swap! local assoc :height (aget js/document "body" "scrollHeight")))]
    (update-dim)
    (aset js/window "onresize" update-dim)
    [:div
     [:svg {:width  (:width @local)
            :height (:height @local)}
      (trailing-circles state-snapshot)]]))

(defn init-fn
  "Listen to onmousemove events for entire page, emit message when fired.
  These events are then sent to the server for measuring the round-trip time,
  and also recorded in the local application state for showing the local mouse
  position."
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
