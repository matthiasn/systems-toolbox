(ns matthiasn.systems-toolbox.ui.observer
  (:require [reagent.core :as r :refer [atom]]
            [clojure.set :as s]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id request-animation-frame]]
            [cljsjs.d3]))

(defn now [] (.getTime (js/Date.)))
(defn r [] (.random js/Math))

(defn nodes-fn
  [nodes-list]
  (map (fn [k] {:name          (if (namespace k) (str (namespace k) "/" (name k)) (name k))
                :key           k :group 1
                :x             (if (= k :client/switchboard) 500 (+ (* 800 (r)) 100))
                :y             (if (= k :client/switchboard) 500 (+ (* 800 (r)) 100))
                :last-received (now)})
       nodes-list))

(defn nodes-map-fn
  [nodes]
  (into {} (map-indexed (fn [idx itm] [(:key itm) (merge itm {:idx idx})])
                        nodes)))

(defn links-fn
  [nodes-map links]
  (into [] (map (fn [m] {:source (:idx ((:from m) nodes-map))
                         :target (:idx ((:to m) nodes-map))}) links)))

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
       [:rect {:x -60
               :y -25
               :width 120
               :height 50
               :rx 5
               :ry 5
               :fill :white
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

(defn in-interval?
  [lower n upper]
  (and (>= n lower) (<= n upper)))

(defn tick-fn
  [app node]
  (let [state @app
        force-cfg (:force-cfg state)
        fixed-nodes (:fixed-nodes force-cfg)]
    (fn [e]
      (let [k (* .1 (.-alpha e))]
        (-> node
            (.attr "transform" (fn [d]
                                 (let [x (.-x d)
                                       y (.-y d)
                                       k (keyword (.-name d))]
                                   (if (contains? fixed-nodes k)
                                     (do
                                       (swap! app assoc-in [:nodes-map k :x] (:x (k fixed-nodes)))
                                       (swap! app assoc-in [:nodes-map k :y] (:y (k fixed-nodes))))
                                     (do
                                       (when (in-interval? 100 x 900)
                                         (swap! app assoc-in [:nodes-map k :x] x))
                                       (when (in-interval? 100 y 900)
                                         (swap! app assoc-in [:nodes-map k :y] y)))))
                                 "")))))))

(defn render-d3-force
  [app dom-id]
  (let [state @app
        force-cfg (:force-cfg state)
        nodes-js (clj->js (:nodes state))
        links-js (clj->js (:d3-links state))
        svg (-> js/d3
                (.select (str "#" dom-id))
                (.append "svg"))
        force (-> js/d3
                  .-layout
                  (.force)
                  (.gravity 0)
                  (.charge (:charge force-cfg))
                  (.linkDistance (:link-distance force-cfg))
                  ;(.chargeDistance (:charge-distance force-cfg))
                  (.size (clj->js [(:width force-cfg) (:height force-cfg)])))
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
        (.on "tick" (tick-fn app node)))))

(defn force-view
  "Renders SVG with an area in which components of a system are shown as a visual representation. These
  visual representations aim at helping in observing a running system."
  [app put-fn]
  (let [nodes-map (:nodes-map @app)
        links (:links @app)]
    [:div
     [:svg {:width "100%" :viewBox "0 0 1000 1000"}
      [:g
       (for [l links]
         ^{:key (str "force-link-" l)}
         [:line.link {:stroke       (condp = (:type l)
                                      :sub "#00CC33"
                                      :tap "#0033CC"
                                      :fh-tap "#CC0033")
                      :stroke-width "3px"
                      :x1           (:x ((:from l) nodes-map))
                      :x2           (:x ((:to l) nodes-map))
                      :y1           (:y ((:from l) nodes-map))
                      :y2           (:y ((:to l) nodes-map))}])
       (for [[k v] nodes-map]
         ^{:key (str "force-node-" k)}
         [cmp-node app v k])]]]))

(defn mk-state
  "Return clean initial component state atom."
  [dom-id force-cfg]
  (fn
    [put-fn]
    (let [app (atom {:time        (now)
                     :layout-done false
                     :force-cfg   force-cfg})
          force-elem (by-id dom-id)]
      (r/render-component [force-view app put-fn force-elem] force-elem)
      (letfn [(step []
                    (request-animation-frame step)
                    (swap! app assoc :now (now)))]
        (request-animation-frame step))
      app)))

(defn count-msg
  "Creates a handler function for collecting stats about messages and display inside the for"
  [ts-key count-key]
  (fn
    [{:keys [cmp-state msg-payload cmp-id]}]
    (let [other-id (:cmp-id msg-payload)]
      (swap! cmp-state assoc-in [:nodes-map other-id ts-key] (now))
      (swap! cmp-state update-in [:nodes-map other-id count-key] #(inc (or % 0)))
      (swap! cmp-state assoc-in [:nodes-map cmp-id :last-rx] (now))
      (swap! cmp-state update-in [:nodes-map cmp-id :rx-count] #(inc (or % 0))))))

(defn state-pub-handler
  "Handle incoming messages: process / add to application state."
  [dom-id]
  (fn
    [{:keys [cmp-state msg-payload]}]
    (swap! cmp-state assoc :switchboard-state msg-payload)
    (when-not (:layout-done @cmp-state)
      (let [switchboard-state (:switchboard-state @cmp-state)
            nodes (nodes-fn (keys (:components switchboard-state)))
            nodes-map (nodes-map-fn nodes)
            subscriptions-set (:subs switchboard-state)
            taps-set (:taps switchboard-state)
            fh-taps-set (:fh-taps switchboard-state)
            links (s/union subscriptions-set taps-set fh-taps-set)
            d3-links (links-fn nodes-map links)]
        (swap! cmp-state assoc :nodes nodes)
        (swap! cmp-state assoc :nodes-map nodes-map)
        (swap! cmp-state assoc :links links)
        (swap! cmp-state assoc :d3-links d3-links)
        (render-d3-force cmp-state dom-id)
        (swap! cmp-state assoc :layout-done true)))))

(defn component
  [cmp-id dom-id force-cfg]
  (comp/make-component {:cmp-id            cmp-id
                        :state-fn          (mk-state dom-id force-cfg)
                        :handler-map       {:firehose/cmp-put           (count-msg :last-tx :tx-count)
                                            :firehose/cmp-publish-state (count-msg :last-tx :tx-count)
                                            :firehose/cmp-recv          (count-msg :last-rx :rx-count)
                                            :firehose/cmp-recv-state    (count-msg :last-rx :rx-count)}
                        :state-pub-handler (state-pub-handler dom-id)
                        :opts              {:snapshots-on-firehose false}}))
