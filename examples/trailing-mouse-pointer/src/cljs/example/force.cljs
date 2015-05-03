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

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [svg (-> js/d3
                (.select "#d3")
                (.append "svg")
                (.attr "width" 960)
                (.attr "height" 600))
        nodes (clj->js [{:name ":srv/sb-cmp" :group 0}
                        {:name ":srv/ws-cmp" :group 0}
                        {:name ":srv/metrics-cmp" :group 0}
                        {:name ":srv/ptr-cmp" :group 0}
                        {:name ":client/ws-cmp" :group 1}
                        {:name ":client/sb-cmp" :group 1}
                        {:name ":client/mouse-cmp" :group 1}
                        {:name ":client/hist-cmp" :group 1}
                        {:name ":client/stats-cmp" :group 1}])
        links (clj->js [{:source 1 :target 0 :value 1}
                        {:source 1 :target 4 :value 8}
                        {:source 0 :target 3 :value 8}
                        {:source 2 :target 0 :value 10}
                        {:source 4 :target 5 :value 6}
                        {:source 5 :target 6 :value 1}
                        {:source 7 :target 5 :value 1}
                        {:source 5 :target 8 :value 1}])
        force (-> js/d3
                  .-layout
                  (.force)
                  (.gravity 0.05)
                  (.distance 70)
                  (.charge -2000)
                  (.size (clj->js [960 500])))
        link (-> svg
                 (.selectAll ".link")
                 (.data links)
                 (.enter)
                 (.append "line")
                 (.attr "class" "link")
                 (.attr "stroke" "#BBB")
                 (.attr "stroke-width" "3px")
                 )
        node (-> svg
                 (.selectAll ".node")
                 (.data nodes)
                 (.enter)
                 (.append "g")
                 (.attr "class" "node")
                 (.call (.drag force)))]
    (-> force
        (.nodes nodes)
        (.links links)
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
        (.attr "fill" "rgba(0, 50, 100, 0.5)")
        (.attr "x" -55)
        (.attr "y" -15))
    (-> node
        (.append "text")
        (.attr "dy" ".35em")
        (.attr "text-anchor" "middle")
        (.attr "text-rendering" "geometricPrecision")
        (.style "font-size" "11px")
        (.style "font-weight" "bold")
        (.text #(.-name %))))
  (let [app (atom {:nodes [] :links []})]
    app))

(defn component
  [cmp-id]
  (comp/make-component cmp-id mk-state in-handler nil))