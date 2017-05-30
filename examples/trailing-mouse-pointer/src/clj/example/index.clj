(ns example.index
  (:require
    [hiccup.core :refer [html]]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [dev?]
  (html
    [:html
     {:lang "en"}
     [:head
      [:meta {:name "viewport" :content "width=device-width, minimum-scale=1.0"}]
      [:title "Systems-Toolbox: Trailing Mouse Pointer Example"]
      [:link {:href "/css/tufte-css/tufte.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/css/example.css" :media "screen" :rel "stylesheet"}]
      [:link {:href "/images/favicon.png"
              :rel  "shortcut icon"
              :type "image/png"}]]
     [:body
      [:div#ui]
      [:div#jvm-stats-frame]
      [:script {:src "/js/build/example.js"}]
      ; Google Analytics for tracking demo page
      [:script {:async "" :src "https://www.google-analytics.com/analytics.js"}]
      [:script (str "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                 (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
                    m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
                                    })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
                 ga('create', 'UA-40261983-6', 'auto'); ga('send', 'pageview');")]]]))

(def sente-map
  "Configuration map for sente-cmp."
  {:index-page-fn index-page
   :port          8763
   :relay-types   #{:mouse/pos :mouse/hist}})
