(ns example.ui-mouse-moves
  (:require [re-frame.core :refer [subscribe]]
            [reagent.core :as rc]))

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
  []
  (let [local-pos (subscribe [:local])
        from-server (subscribe [:from-server])]
    (fn []
      [:g
       [:circle (merge circle-defaults {:cx (:x @local-pos)
                                        :cy (:y @local-pos)})]
       [:circle (merge circle-defaults {:cx   (:x @from-server)
                                        :cy   (:y @from-server)
                                        :fill "rgba(0,0,255,0.1)"})]])))

(defn mouse-view
  "Renders SVG with both local mouse position and the last one returned from the
   server, in an area that covers the entire visible page."
  []
  (let [local (rc/atom {})
        update-dim (fn [_ev]
                     (let [h 3000
                           w (.-innerWidth js/window)]
                       (swap! local assoc :width w)
                       (swap! local assoc :height h)))]
    (update-dim nil)
    (aset js/window "onresize" update-dim)
    (fn mouse-view-render []
      [:div
       [:svg {:width  (:width @local)
              :height (:height @local)}
        [trailing-circles]
        #_#_(when (-> state-snapshot :show-all :local)
              [mouse-hist-view state-snapshot :local-hist
               "rgba(0,0,0,0.06)" "rgba(0,255,0,0.05)"])
            (when (-> state-snapshot :show-all :server)
              [mouse-hist-view state-snapshot :server-hist
               "rgba(0,0,0,0.06)" "rgba(0,0,128,0.05)"])]])))
