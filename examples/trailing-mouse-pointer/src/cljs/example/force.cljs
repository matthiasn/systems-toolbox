(ns example.force
  (:require [reagent.core :as r :refer [atom]]
            [cljs.pprint :as pp]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id request-animation-frame]]
            [cljs.core.match :refer-macros [match]]))

(defn now [] (.getTime (js/Date.)))
(defn r [] (.random js/Math))

(defn nodes-fn
  [nodes-list]
  (map (fn [k] {:name (if (namespace k)
                        (str (namespace k) "/" (name k))
                        (name k))
                :key k :group 1 :x (r) :y (r) :last-received (now)})
                   nodes-list))

(defn nodes-map-fn
  [nodes]
  (into {} (map-indexed (fn [idx itm] [(:key itm) (merge itm {:idx idx })])
                        nodes)))

(def links-vec [{:source :client/ws-cmp :target :client/switchboard :value 6}
                {:source :client/ws-cmp :target :client/mouse-cmp :value 1}
                {:source :client/store-cmp :target :client/mouse-cmp :value 1}
                {:source :client/ws-cmp :target :client/store-cmp :value 1}
                {:source :client/store-cmp :target :client/histogram-cmp :value 1}
                {:source :client/ws-cmp :target :client/jvmstats-cmp :value 1}
                {:source :client/ws-cmp :target :client/switchboard :value 1}
                {:source :client/store-cmp :target :client/switchboard :value 1}
                {:source :client/log-cmp :target :client/switchboard :value 1}
                {:source :client/mouse-cmp :target :client/switchboard :value 1}
                {:source :client/jvmstats-cmp :target :client/switchboard :value 1}
                {:source :client/force-cmp :target :client/switchboard :value 1}
                {:source :client/histogram-cmp :target :client/switchboard :value 1}])

(defn links-fn
  [nodes-map]
  (into [] (map (fn [m] {:source (:idx ((:source m) nodes-map))
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
                  (.distance 150)
                  (.charge -10000)
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

(defn force-view
  "Renders SVG with an area in which components of a system are shown as a visual representation. These
  visual representations aim at helping in observing a running system."
  [app put-fn]
  (let [nodes-map (:nodes-map @app)]
    [:div
     [:svg {:width "100%" :viewBox "0 0 1000 1000"}
      [:g
       (for [m links-vec]
         ^{:key (str "force-link-" m)}
         [:line.link {:stroke "#BBB" :stroke-width "3px"
                      :x1     (:x ((:source m) nodes-map))
                      :x2     (:x ((:target m) nodes-map))
                      :y1     (:y ((:source m) nodes-map))
                      :y2     (:y ((:target m) nodes-map))}])
       (for [[k v] nodes-map]
         ^{:key (str "force-node-" k)}
         [cmp-node app v k])]]
     [:div (str "Components: " (keys (:components (:switchboard-state @app))))]
     [:div (str "Nodes: " (str (:nodes @app)))]
     [:div (str "Nodes2: " (str (:nodes2 @app)))]
     [:div (str "Nodes-Map: " (str (:nodes-map @app)))]
     [:div (str "Nodes-Map2: " (str (:nodes-map2 @app)))]
     [:div (str "Links: " (str (:links @app)))]
     [:div (str "Links2: " (str (:links2 @app)))]
     [:div (str "Subscriptions: " (:subs (:switchboard-state @app)))]
     [:div (str "Taps: " (:taps (:switchboard-state @app)))]]))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [app (atom {:time        (now)
                   :layout-done false})
        force-elem (by-id "force")]
    (r/render-component [force-view app put-fn force-elem] force-elem)
    (letfn [(step []
                  (request-animation-frame step)
                  (swap! app assoc :now (now)))]
      (request-animation-frame step))
    app))

(defn count-msg
  "Creates a handler function for collecting stats about messages and display inside the for"
  [ts-key count-key]
  (fn
    [{:keys [cmp-state msg-payload]}]
    (let [cmp-id (:cmp-id msg-payload)]
      (swap! cmp-state assoc-in [:nodes-map cmp-id ts-key] (now))
      (swap! cmp-state update-in [:nodes-map cmp-id count-key] #(inc (or % 0))))))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg-payload]}]
  (swap! cmp-state assoc :switchboard-state msg-payload)
  (when-not (:layout-done @cmp-state)
    (let [nodes (nodes-fn (keys (:components (:switchboard-state @cmp-state))))
          nodes-map (nodes-map-fn nodes)
          links (links-fn nodes-map)]
      (swap! cmp-state assoc :nodes nodes)
      (swap! cmp-state assoc :nodes-map nodes-map)
      (swap! cmp-state assoc :links links)
      (render-d3-force cmp-state)
      (swap! cmp-state assoc :layout-done true))))

(defn component
  [cmp-id]
  (comp/make-component {:cmp-id            cmp-id
                        :state-fn          mk-state
                        :handler-map       {:firehose/cmp-put  (count-msg :last-tx :tx-count)
                                            :firehose/cmp-recv (count-msg :last-rx :rx-count)}
                        :state-pub-handler state-pub-handler}))
