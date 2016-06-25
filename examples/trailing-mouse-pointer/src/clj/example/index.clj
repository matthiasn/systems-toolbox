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
      [:link {:href "/css/tufte-css/tufte.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/css/example.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/images/favicon.png", :rel "shortcut icon", :type "image/png"}]]
     [:body
      [:div#mouse]
      [:article
       [:h1 "WebSockets Latency Visualization"]
       [:p [:a {:href "https://github.com/matthiasn" :target "_blank"} "Matthias Nehlsen"]]
       [:section
        [:p "This is the second example in the "
         [:a {:href   "https://leanpub.com/building-a-system-in-clojure"
              :target "_blank"} "Building Systems in Clojure(Script)"]
         " book. It gives you a feel for how fast communication via WebSocket is. Move your mouse anywhere on the page.
          The mouse move events will then be sent to the server and returned over the WebSocket
          connection and the time for sending and returning each message is measured. These times are then
          shown in histograms."]
        [:figure#histograms.fullwidth]
        [:div#info]
        [:figure#observer]]
       [:p "The histograms above are entirely drawn in ClojureScript - without any additional charting library.
            The number of bins is determined by applying the "
        [:a {:href "http://en.wikipedia.org/wiki/Freedman–Diaconis_rule"} "Freedman-Diaconis rule"]
        ". The first histogram takes the entire sample into account whereas the second only dispays the observations
         that fall within the 99th percentile in order to remove potential outliers."]

       [:p "Check out the code on "
        [:a {:href "https://github.com/matthiasn/systems-toolbox" :target "_blank"} "GitHub"] "."]

       [:div#jvm-stats-frame]]
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
   :relay-types   #{:mouse/pos :stats/jvm :mouse/hist}})
