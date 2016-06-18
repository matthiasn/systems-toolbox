(ns example.index
  (:require [hiccup.core :refer [html]]))

(defn index-page
  "Generates index page HTML."
  [_]
  (html
    [:html
     {:lang "en"}
     [:head
      [:meta {:name "viewport" :content "width=device-width, minimum-scale=1.0"}]
      [:title "Counter"]
      [:link {:href "/css/example.css" :rel "stylesheet"}]]
     [:body
      [:div#counter]
      [:script {:src "/js/build/example.js"}]]]))

(def sente-map
  "Configuration map for sente-cmp."
  {:index-page-fn index-page
   :relay-types   #{}})
