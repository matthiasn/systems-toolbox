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

(def nodes [{:name "srv/sb-cmp" :group 0 :x 250 :y 250}
            {:name "srv/ws-cmp" :group 0 :x 400 :y 250}
            {:name "srv/metrics-cmp" :group 0 :x 150 :y 150}
            {:name "srv/ptr-cmp" :group 0 :x 150 :y 350}
            {:name "client/ws-cmp" :group 1 :x 600 :y 250}
            {:name "client/sb-cmp" :group 1 :x 750 :y 250}
            {:name "client/mouse-cmp" :group 1 :x 800 :y 150}
            {:name "client/hist-cmp" :group 1 :x 800 :y 250}
            {:name "client/stats-cmp" :group 1 :x 800 :y 350}])

;(def nodes (for [[k v] nodes-map] (merge v {:name (str (namespace k) "/" (name k))})))

(def nodes-map (into {} (map (fn [m] [(keyword (:name m)) m]) nodes)))

(def links [{:source 1 :target 0 :value 1}
            {:source 1 :target 4 :value 8}
            {:source 0 :target 3 :value 8}
            {:source 2 :target 0 :value 10}
            {:source 4 :target 5 :value 6}
            {:source 5 :target 6 :value 1}
            {:source 7 :target 5 :value 1}
            {:source 5 :target 8 :value 1}])

(def links-vec [{:source :srv/ws-cmp :target :srv/sb-cmp :value 1}
                {:source :srv/ws-cmp :target :client/ws-cmp :value 8}
                {:source :srv/sb-cmp :target :srv/ptr-cmp :value 8}
                {:source :srv/metrics-cmp :target :srv/sb-cmp :value 10}
                {:source :client/ws-cmp :target :client/sb-cmp :value 6}
                {:source :client/sb-cmp :target :client/mouse-cmp :value 1}
                {:source :client/hist-cmp :target :client/sb-cmp :value 1}
                {:source :client/sb-cmp :target :client/stats-cmp :value 1}])

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
        nodes (:nodes @app)
        nodes-map (:nodes-map @app)]
    [:div.pure-u-1
     [:svg {:width "100%" :viewBox "0 0 960 500"}
      [:g
       (for [m links-vec]
         ^{:key (str "force-link-" m)}
         [:line.link {:stroke "#BBB" :stroke-width "3px"
                      :x1 (:x ((:source m) nodes-map))
                      :x2 (:x ((:target m) nodes-map))
                      :y1 (:y ((:source m) nodes-map))
                      :y2 (:y ((:target m) nodes-map))
                      }])
       (for [[k v] nodes-map]
         ^{:key (str "force-node-" k)}
         [cmp-node (:x v) (:y v) (str k) (:group v)])]]]))

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
;                          (.attr "transform" #(str "translate(" (.-x %) "," (.-y %) ")"))
                          (.attr "transform" (fn [d]
                                               (let [x (.-x d)
                                                     y (.-y d)
                                                     k (keyword (.-name d))]
                                                  (swap! app assoc-in [:nodes-map k :x] x)
                                                  (swap! app assoc-in [:nodes-map k :y] y)
                                                 (str "translate(" x "," y ")"))))
                          ))))
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
  (let [app (atom {:nodes nodes :links links :nodes-map nodes-map})
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