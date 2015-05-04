(ns example.force
  (:require [reagent.core :as r :refer [atom]]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id]]
            [cljs.core.match :refer-macros [match]]))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         :else (prn "unknown msg in data-loop" msg)))

(def nodes-map {:srv/sb-cmp {:group 0 :x 250 :y 250}
                :srv/ws-cmp {:group 0 :x 400 :y 250}
                :srv/metrics-cmp {:group 0 :x 150 :y 150}
                :srv/ptr-cmp {:group 0 :x 150 :y 350}
                :client/ws-cmp {:group 1 :x 600 :y 250}
                :client/sb-cmp {:group 1 :x 750 :y 250}
                :client/mouse-cmp {:group 1 :x 800 :y 150}
                :client/hist-cmp {:group 1 :x 800 :y 250}
                :client/stats-cmp {:group 1 :x 800 :y 350}})

(def nodes (for [[k v] nodes-map] (merge v {:name (str k)})))

(def links [{:source 1 :target 0 :value 1}
            {:source 1 :target 4 :value 8}
            {:source 0 :target 3 :value 8}
            {:source 2 :target 0 :value 10}
            {:source 4 :target 5 :value 6}
            {:source 5 :target 6 :value 1}
            {:source 7 :target 5 :value 1}
            {:source 5 :target 8 :value 1}])

(defn cmp-node
  [x y name grp]
  [:g {:transform (str "translate(" x "," y ")")}
   [:rect {:x -55 :y -15 :width 110 :height 30 :rx 5 :ry 5 :fill :white
           :stroke (if (zero? grp) "#C55" "#5C5") :stroke-width "2px"}]
   [:text {:dy   ".35em" :text-anchor :middle :text-rendering "geometricPrecision" :stroke :none
           :fill :black :font-size "11px" :font-weight :bold} name]])

(defn force-view
  "Renders SVG with an area in which components of a system are shown as a visual representation. These
  visual representations aim at helping in observing a running system."
  [app put-fn]
  (let [force-div (by-id "force")
        local (atom {})
        nodes (:nodes @app)]
    [:div.pure-u-1
     [:svg {:width "100%" :viewBox "0 0 960 500"}
      [:g
       (for [n nodes]
         ^{:key (str "force-" (:name n))}
         [cmp-node (:x n) (:y n) (:name n) (:group n)])]]]))

(defn render-d3-force
  [app]
  (let [state @app
        nodes-js (clj->js (:nodes state))
        links-js (clj->js (:links state))
        svg (-> js/d3
                (.select "#d3")
                (.append "svg")
                (.attr "width" 960)
                (.attr "height" 600))
        force (-> js/d3
                  .-layout
                  (.force)
                  (.gravity 0.05)
                  (.distance 90)
                  (.charge -2000)
                  (.size (clj->js [960 500])))
        link (-> svg
                 (.selectAll ".link")
                 (.data links-js)
                 (.enter)
                 (.append "line")
                 (.attr "class" "link")
                 (.attr "stroke" "#BBB")
                 (.attr "stroke-width" "3px")
                 )
        node (-> svg
                 (.selectAll ".node")
                 (.data nodes-js)
                 (.enter)
                 (.append "g")
                 (.attr "class" "node")
                 (.call (.drag force)))]
    (-> force
        (.nodes nodes-js)
        (.links links-js)
        (.start))
    (-> force
        (.on "tick" (fn [_]
                      (-> link
                          (.attr "x1" #(.-x (.-source %)))
                          (.attr "x2" #(.-x (.-target %)))
                          (.attr "y1" #(.-y (.-source %)))
                          (.attr "y2" #(.-y (.-target %))))
                      (-> node
                          (.attr "transform" #(str "translate(" (.-x %) "," (.-y %) ")"))))))
    (-> node
        (.append "rect")
        (.attr "width" 110)
        (.attr "height" 30)
        (.attr "fill" "white")
        (.attr "stroke" #(if (zero? (.-group %)) "#C55" "#5C5"))
        (.attr "stroke-width" "2px")
        (.attr "x" -55)
        (.attr "y" -15)
        (.attr "rx" 5)
        (.attr "ry" 5))
    (-> node
        (.append "text")
        (.attr "dy" ".35em")
        (.attr "text-anchor" "middle")
        (.attr "text-rendering" "geometricPrecision")
        (.attr "stroke" "none")
        (.attr "fill" "black")
        (.style "font-size" "11px")
        (.style "font-weight" "bold")
        (.text #(.-name %)))))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:nodes nodes :links links})
        force-elem (by-id "force")]
    (render-d3-force app)
    (r/render-component [force-view app put-fn force-elem] force-elem)
    app))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [app _ [_ state-snapshot]])

(defn component
  [cmp-id]
  (comp/make-component cmp-id mk-state in-handler state-pub-handler))