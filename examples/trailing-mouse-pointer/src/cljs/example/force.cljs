(ns example.force
  (:require [reagent.core :as r :refer [atom]]
            [cljs.pprint :as pp]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id request-animation-frame]]
            [cljs.core.match :refer-macros [match]]))

(defn now [] (.getTime (js/Date.)))

(def nodes [
            ;{:name "server/switchboard" :group 0 :x 250 :y 250 :last-received (now)}
            ;{:name "server/ws-cmp" :group 0 :x 400 :y 250 :last-received (now)}
            ;{:name "server/metrics-cmp" :group 0 :x 150 :y 150 :last-received (now)}
            ;{:name "server/scheduler-cmp" :group 0 :x 150 :y 450 :last-received (now)}
            ;{:name "server/ptr-cmp" :group 0 :x 150 :y 350 :last-received (now)}
            {:name "client/ws-cmp" :group 1 :x 600 :y 250 :last-received (now)}
            {:name "client/switchboard" :group 1 :x 750 :y 250 :last-received (now)}
            {:name "client/mouse-cmp" :group 1 :x 800 :y 150 :last-received (now)}
            {:name "client/store-cmp" :group 1 :x 800 :y 400 :last-received (now)}
            {:name "client/force-cmp" :group 1 :x 800 :y 50 :last-received (now)}
            {:name "client/histogram-cmp" :group 1 :x 800 :y 350 :last-received (now)}
            {:name "client/jvmstats-cmp" :group 1 :x 800 :y 550 :last-received (now)}])

;(def nodes (for [[k v] nodes-map] (merge v {:name (str (namespace k) "/" (name k))})))

(def nodes-map (into {} (map-indexed (fn [idx itm] [(keyword (:name itm)) (merge itm {:idx idx})]) nodes)))

(def links-vec [
                ;{:source :server/ws-cmp :target :server/switchboard :value 1}
                ;{:source :server/ws-cmp :target :client/ws-cmp :value 8}
                ;{:source :server/switchboard :target :server/ptr-cmp :value 8}
                ;{:source :server/switchboard :target :server/scheduler-cmp :value 8}
                ;{:source :server/metrics-cmp :target :server/switchboard :value 10}

                {:source :client/ws-cmp :target :client/switchboard :value 6}

                ;{:source :client/switchboard :target :client/mouse-cmp :value 1}
                {:source :client/ws-cmp :target :client/mouse-cmp :value 1}
                {:source :client/store-cmp :target :client/mouse-cmp :value 1}

                ;{:source :client/switchboard :target :client/store-cmp :value 1}
                {:source :client/ws-cmp :target :client/store-cmp :value 1}

                ;{:source :client/switchboard :target :client/scheduler-cmp :value 1}

                ;{:source :client/histogram-cmp :target :client/switchboard :value 1}
                {:source :client/store-cmp :target :client/histogram-cmp :value 1}

                ;{:source :client/switchboard :target :client/jvmstats-cmp :value 1}
                {:source :client/ws-cmp :target :client/jvmstats-cmp :value 1}

                {:source :client/ws-cmp :target :client/switchboard :value 1}
                {:source :client/store-cmp :target :client/switchboard :value 1}
                {:source :client/mouse-cmp :target :client/switchboard :value 1}
                {:source :client/jvmstats-cmp :target :client/switchboard :value 1}

                {:source :client/force-cmp :target :client/switchboard :value 1}
                {:source :client/histogram-cmp :target :client/switchboard :value 1}

                ])

(def links (into [] (map (fn [m] {:source (:idx ((:source m) nodes-map))
                                  :target (:idx ((:target m) nodes-map))}) links-vec)))

(def foci [{:x 150 :y 150} {:x 350 :y 250}])

(defn cmp-node
  [app node cmp-key]
  (let [x (:x node)
        y (:y node)
        grp (:group node)
        ms-since-last (- (:now @app) (:last-received node))
        ms-since-rx (- (:now @app) (:last-rx node))
        ms-since-tx (- (:now @app) (:last-tx node))
        rx-cnt (:rx-count node)
        tx-cnt (:tx-count node)]
    (when x
      [:g {:transform (str "translate(" x "," y ")")
           :on-click  #(prn ((-> @app (:switchboard-state) (:components) (cmp-key) (:state-snapshot-fn))))}
       [:rect {:x      -60 :y -25 :width 120 :height 50 :rx 5 :ry 5 :fill :white
               :stroke (if (zero? grp) "#C55" "#5C5") :stroke-width "2px"}]
       [:text {:dy   "-.5em" :text-anchor :middle :text-rendering "geometricPrecision" :stroke :none
               :fill :black :font-size "11px" :style {:font-weight :bold}} (str cmp-key)]
       [:text {:dy   "1em" :text-anchor :middle :text-rendering "geometricPrecision" :stroke :none
               :fill :gray :font-size "11px" :style {:font-weight :bold}}
        (str (when rx-cnt (str "rx: " rx-cnt)) (when tx-cnt (str " tx: " tx-cnt)))]
       [:rect {:x     44 :y 5 :width 10 :height 10 :rx 1 :ry 1 :fill :green
               :style {:opacity (let [opacity (/ (max 0 (- 250 ms-since-tx)) 250)]
                                  opacity)}}]
       [:rect {:x     -54 :y 5 :width 10 :height 10 :rx 1 :ry 1 :fill :orangered
               :style {:opacity (let [opacity (/ (max 0 (- 250 ms-since-rx)) 250)]
                                  opacity)}}]])))

(defn force-view
  "Renders SVG with an area in which components of a system are shown as a visual representation. These
  visual representations aim at helping in observing a running system."
  [app put-fn]
  (let [nodes-map (:nodes-map @app)
        nodes (map :cmp-id (-> @app (:switchboard-state) (:components)))]
    [:div
     [:svg {:width "100%" :viewBox "0 0 1000 1000"}
      [:g
       (for [m links-vec]
         ^{:key (str "force-link-" m)}
         [:line.link {:stroke "#BBB"  :stroke-width "3px"
                      :x1 (:x ((:source m) nodes-map))
                      :x2 (:x ((:target m) nodes-map))
                      :y1 (:y ((:source m) nodes-map))
                      :y2 (:y ((:target m) nodes-map))}])
       (for [[k v] nodes-map]
         ^{:key (str "force-node-" k)}
         [cmp-node app v k])]]]))

(defn render-d3-force
  [app]
  (let [state @app
        nodes-js (clj->js (:nodes state))
        links-js (clj->js (:links state))
        svg (-> js/d3
                (.select "#d3")
                (.append "svg"))
        force (-> js/d3
                  .-layout
                  (.force)
                  ;(.gravity 0.05)
                  (.distance 140)
                  (.charge -7000)
                  (.size (clj->js [900 900])))
        node (-> svg
                 (.selectAll ".node")
                 (.data nodes-js)
                 (.enter)
                 (.append "g"))]
    (-> force
        (.nodes nodes-js)
        (.links links-js)
        (.start))
    (-> force
        (.on "tick" (fn [e]
                      (let [k (* .1 (.-alpha e))]
                        (-> node
                            (.attr "transform" (fn [d]
                                                 (let [x (.-x d)
                                                       y (.-y d)
                                                       k (keyword (.-name d))]
                                                   (swap! app assoc-in [:nodes-map k :x] x)
                                                   (swap! app assoc-in [:nodes-map k :y] y))
                                                 "")))))))))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:nodes nodes :links links :nodes-map nodes-map :time (now)})
        force-elem (by-id "force")]
    (render-d3-force app)
    (r/render-component [force-view app put-fn force-elem] force-elem)
    (letfn [(step []
                  (request-animation-frame step)
                  (swap! app assoc :now (now)))]
      (request-animation-frame step))
    app))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [app put-fn msg]
  (match msg
         [:firehose/cmp-put  m] (let [cmp-id (:cmp-id m)]
                                  (swap! app assoc-in [:nodes-map cmp-id :last-tx] (now))
                                  (swap! app update-in [:nodes-map cmp-id :tx-count] #(inc (or % 0))))
         [:firehose/cmp-recv m] (let [cmp-id (:cmp-id m)]
                                  (swap! app assoc-in [:nodes-map cmp-id :last-rx] (now))
                                  (swap! app update-in [:nodes-map cmp-id :rx-count] #(inc (or % 0))))
         :else (prn "unknown msg in :force-cmp in handler" msg)))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [app _ [_ state-snapshot]]
  (swap! app assoc :switchboard-state state-snapshot))

(defn component
  [cmp-id]
  (comp/make-component {:cmp-id   cmp-id
                        :state-fn mk-state
                        :handler  in-handler
                        :state-pub-handler state-pub-handler}))