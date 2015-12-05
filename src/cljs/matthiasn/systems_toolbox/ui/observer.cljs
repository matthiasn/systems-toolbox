(ns matthiasn.systems-toolbox.ui.observer
  (:require [reagent.core :as r :refer [atom]]
            [clojure.set :as s]
            [matthiasn.systems-toolbox.component :as comp]
            [matthiasn.systems-toolbox.helpers :refer [by-id request-animation-frame]]))

(defn now [] (.getTime (js/Date.)))
(defn r [] (.random js/Math))

(defn nodes-map-fn
  [nodes-list obs-cfg]
  (let [nodes (map (fn [k]
                     (let [fixed-nodes (:fixed-nodes obs-cfg)]
                       {:name          (if (namespace k) (str (namespace k) "/" (name k)) (name k))
                        :key           k
                        :x             (if (contains? fixed-nodes k) (-> fixed-nodes k :x) (+ (* 800 (r)) 100))
                        :y             (if (contains? fixed-nodes k) (-> fixed-nodes k :y) (+ (* 800 (r)) 100))
                        :last-received (now)}))
                   nodes-list)]
    (into {} (map (fn [itm] [(:key itm) itm]) nodes))))

(defn links-fn
  [nodes-map links]
  (vec (map (fn [m] {:source (:idx ((:from m) nodes-map))
                         :target (:idx ((:to m) nodes-map))}) links)))

(defn cmp-node
  [app node cmp-key]
  (let [x (:x node)
        y (:y node)
        grp (:group node)
        ms-since-rx (- (:now @app) (:last-rx node))
        ms-since-tx (- (:now @app) (:last-tx node))
        rx-cnt (:rx-count node)
        tx-cnt (:tx-count node)]
    (when x
      [:g {:transform (str "translate(" x "," y ")")
           :on-click  #(prn ((-> @app (:switchboard-state) (:components) (cmp-key) (:state-snapshot-fn))))}
       [:rect {:x      -60
               :y      -25
               :width  120
               :height 50
               :rx     5
               :ry     5
               :fill   :white
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

(defn system-view
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
  [obs-cfg]
  (fn
    [put-fn]
    (let [app (atom {:time    (now)
                     :obs-cfg obs-cfg})
          system-view-elem (by-id (:dom-id obs-cfg))]
      (r/render-component [system-view app put-fn system-view-elem] system-view-elem)
      (letfn [(step []
                    (request-animation-frame step)
                    (swap! app assoc :now (now)))]
        (request-animation-frame step))
      {:state app})))

(defn count-msg
  "Creates a handler function for collecting stats about messages and and their display."
  [ts-key count-key]
  (fn
    [{:keys [cmp-state msg-payload cmp-id]}]
    (let [other-id (:cmp-id msg-payload)]
      (when (:switchboard-state @cmp-state)
        (swap! cmp-state assoc-in [:nodes-map other-id ts-key] (now))
        (swap! cmp-state update-in [:nodes-map other-id count-key] #(inc (or % 0)))
        (swap! cmp-state assoc-in [:nodes-map cmp-id :last-rx] (now))
        (swap! cmp-state update-in [:nodes-map cmp-id :rx-count] #(inc (or % 0)))))))

(defn state-snapshot-handler
  "Creates a handler function for component snapshot messages. Uses messages from switchboard for configuring UI."
  [switchbrd-id]
  (fn
    [{:keys [cmp-state msg-payload] :as msg-map}]
    (let [other-id (:cmp-id msg-payload)
          count-fn (count-msg :last-rx :rx-count)]
      (count-fn msg-map)
      (when (= other-id switchbrd-id)
        (let [switchboard-state (:snapshot msg-payload)
              obs-cfg (:obs-cfg @cmp-state)
              nodes-map (nodes-map-fn (keys (:components switchboard-state)) obs-cfg)
              subscriptions-set (:subs switchboard-state)
              taps-set (:taps switchboard-state)
              fh-taps-set (:fh-taps switchboard-state)
              links (s/union subscriptions-set taps-set fh-taps-set)]
          (swap! cmp-state assoc :switchboard-state switchboard-state)
          (swap! cmp-state assoc :nodes-map nodes-map)
          (swap! cmp-state assoc :links links))))))

(defn cmp-map
  {:added "0.3.1"}
  [cmp-id obs-cfg]
  {:cmp-id      cmp-id
   :state-fn    (mk-state obs-cfg)
   :handler-map {:firehose/cmp-put           (count-msg :last-tx :tx-count)
                 :firehose/cmp-publish-state (state-snapshot-handler (:switchbrd-id obs-cfg))
                 :firehose/cmp-recv          (count-msg :last-rx :rx-count)
                 :firehose/cmp-recv-state    (count-msg :last-rx :rx-count)}
   :opts        {:snapshots-on-firehose false
                 :reload-cmp false}})

(defn component
  {:deprecated "0.3.1"}
  [cmp-id obs-cfg]
  (comp/make-component (cmp-map cmp-id obs-cfg)))
