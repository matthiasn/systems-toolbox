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
      [:div#mouse]
      [:article
       [:h1 "WebSockets Latency Visualization"]
       [:p [:a {:href "https://github.com/matthiasn" :target "_blank"}
            "Matthias Nehlsen"]]
       [:section
        [:p
         "WebSockets bring bi-directional communication to the browser. This
          enables you to deliver interactive, real time web applications where
          all the data is as of right now, rather than always being outdated,
          and then constantly refreshed."]
        [:p
         "But how fast is this transport mechanism? Let's have a look. You may
         have noticed the circle around the mouse pointer on this page, "
         [:label {:for "explain1" :class "margin-toggle"} " ⊕ "]
         [:input#explain1 {:type "checkbox" :class "margin-toggle"}]
         [:span.marginnote
          "What happens here is that movements of the mouse (or your finger on
           your mobile device) are captured. The more reddish one is then painted
           immediately, whereas the bluish one is painted after the event is sent
           to a server somewhere in Germany, and then back to wherever you are."]
         "or in fact the two circles, where one of them appears to follow the other.
          Both represent your last mouse position, only that one was sent to and
          returned from the server in the meantime. This gives you an intuition
          for how long it takes. Also, with your movement of the mouse, you
          generate data for the histograms below, which show the roundtrip duration:"]
        [:figure#histograms.fullwidth]
        [:p
         "Now, since we are already capturing the movement of the mouse, you
          may think that it could be interesting to see where the users' mouses
          go, as a proxy for where they are looking on a page. Surely not as
          accurate as actual eye tracking, but probably much better than nothing.
          Now let's see where your mouse was since you started interacting with
          this page. Click the \"show all\" button in the info section below,"
         [:label {:for "explain2" :class "margin-toggle"} " ⊕ "]
         [:input#explain2 {:type "checkbox" :class "margin-toggle"}]
         [:span.marginnote
          "By clicking those buttons again, you can switch the display on or off."]
         " and you see where your mouse goes. Then, by clicking
          \"show all (server)\", you can also display the most recent mouse
          positions of all visitors on this page."]
        [:div#info]
        [:p
         "You are looking at a web application written in Clojure andClojureScript.
          It is one of the example applications of the systems-toolbox library.
          The histograms above are rendered entirely in ClojureScript
          - without any additional charting library."
         [:span.marginnote "The "
          [:a {:href "http://en.wikipedia.org/wiki/Freedman–Diaconis_rule"}
           "Freedman-Diaconis rule"]
          " determines the number of bins in the histograms. The first
           histogram takes the entire sample into account whereas the second only
           displays the observations that fall within the 99th percentile to
           remove potential outliers."]]
        [:figure
         [:label {:for "fig2" :class "margin-toggle"} " ⊕ "]
         [:input#fig2 {:type "checkbox" :class "margin-toggle"}]
         [:span.marginnote
          "Structure of the ClojureScript application, with their message flow
           visualized as rx and tx LEDs, like on a network card."]
         [:div#observer]]
        [:p
         "If you want to know how this application was built, have a look at the
          code on "
         [:a {:href   "https://github.com/matthiasn/systems-toolbox"
              :target "_blank"} "GitHub"]
         " or the book "
         [:a {:href   "https://leanpub.com/building-a-system-in-clojure"
              :target "_blank"} "Building Systems in Clojure(Script)"]
         ". Also, check for a future blog post on "
         [:a {:href "https://matthiasnehlsen.com" :target "_blank"}
          "matthiasnehlsen.com"]
         "."]
        [:p
         "Finally, if you like the layout of this page, you need to look at "
         [:a {:href "https://edwardtufte.github.io/tufte-css/" :target "_blank"}
          "Tufte CSS"]
         ". It allowed me to write this application with only around 30 lines of
          CSS, most of which is related to the flexbox layout for histogram SVGs."]]
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
   :port          8763
   :relay-types   #{:mouse/pos :stats/jvm :mouse/hist}})
