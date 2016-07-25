(ns example.ui-mouse-moves
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [matthiasn.systems-toolbox-ui.helpers :refer [by-id]]))

;; some SVG defaults
(def circle-defaults {:fill "rgba(255,0,0,0.1)
" :stroke "rgba(0,0,0,0.5)"
                      :stroke-width 2 :r 15})
(def text-default {:stroke "none" :fill "black" :style {:font-size 12}})
(def text-bold (merge text-default {:style {:font-weight :bold :font-size 12}}))

(defn mouse-hist-view
  "Render SVG group with filled circles from a vector of mouse positions in state."
  [state state-key stroke fill]
  (let [positions (map-indexed vector (state-key state))]
    (when (seq positions)
      [:g {:opacity 0.5}
       (for [[idx pos] positions]
         ^{:key (str "circle" state-key idx)}
         [:circle {:stroke       stroke
                   :stroke-width 2
                   :r            15
                   :cx           (:x pos)
                   :cy           (:y pos)
                   :fill         fill}])])))

(defn trailing-circles
  "Displays two transparent circles. The position of the circles comes from
   the most recent messages, one sent locally and the other with a roundtrip to
   the server in between. This makes it easier to visually detect any delays."
  [state]
  (let [local-pos (:local state)
        from-server (:from-server state)]
    [:g
     [:circle (merge circle-defaults {:cx (:x local-pos)
                                      :cy (:y local-pos)})]
     [:circle (merge circle-defaults {:cx (:x from-server)
                                      :cy (:y from-server)
                                      :fill "rgba(0,0,255,0.1)"})]]))

(defn mouse-view
  "Renders SVG with both local mouse position and the last one returned from the
   server, in an area that covers the entire visible page."
  [{:keys [observed local]}]
  (let [state-snapshot @observed
        mouse-div (by-id "mouse")
        update-dim
        #(do (swap! local assoc :width (- (.-offsetWidth mouse-div) 2))
             (swap! local assoc :height (aget js/document "body" "scrollHeight")))]
    (update-dim)
    (aset js/window "onresize" update-dim)
    [:div
     [:svg {:width  (:width @local)
            :height (:height @local)}
      (trailing-circles state-snapshot)
      (when (-> state-snapshot :show-all :local)
        [mouse-hist-view state-snapshot :local-hist
         "rgba(0,0,0,0.06)" "rgba(0,255,0,0.05)"])
      (when (-> state-snapshot :show-all :server)
        [mouse-hist-view state-snapshot :server-hist
         "rgba(0,0,0,0.06)" "rgba(0,0,128,0.05)"])]]))

(defn init-fn
  "Listen to onmousemove events for entire page, emit message when fired.
  These events are then sent to the server for measuring the round-trip time,
  and also recorded in the local application state for showing the local mouse
  position."
  [{:keys [put-fn]}]
  (aset js/window "onmousemove"
        #(put-fn [:mouse/pos {:x (.-pageX %) :y (.-pageY %)}]))
  (aset js/window "ontouchmove"
        (fn [ev]
          (let [t (aget (.-targetTouches ev) 0)]
            (put-fn [:mouse/pos {:x (.-pageX t) :y (.-pageY t)}])
            #_(.preventDefault ev)))))

(defn cmp-map
  "Configuration map for systems-toolbox-ui component."
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :view-fn mouse-view
              :dom-id  "mouse"
              :init-fn init-fn
              :cfg     {:msgs-on-firehose true}}))
