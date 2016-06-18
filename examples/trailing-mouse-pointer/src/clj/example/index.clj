(ns example.index
  (:require
    [hiccup.core :refer [html]]
    [garden.core :refer [css]]
    [garden.units :as u :refer [px]]))

(defn index-css
  "Generate index page CSS in Clojure using Garden."
  []
  (css {:pretty-print? false}
       [:.content {:background-color "#FCFCF0"}]
       [:a {:font-weight :bold
            :text-decoration :none
            :color "#1f8dd6"}]
       [:p {:line-height (px 25)
            :margin-left "10%" :margin-right "10%"}]
       [:#mouse {:width "100%"
                 :cursor :pointer}]
       [:#messages {:padding-right "10px"}]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [dev?]
  (html
    [:html
     {:lang "en"}
     [:head
      ;[:meta {:content "width=device-width, user-scalable=no", :name "viewport"}]
      [:meta {:name "viewport" :content "width=device-width, minimum-scale=1.0"}]
      [:title "Systems-Toolbox: Trailing Mouse Pointer Example"]
      [:link {:href "/bower_components/pure/pure-min.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/bower_components/pure/grids-responsive-min.css", :media "screen", :rel "stylesheet"}]
      [:link {:href "/css/example.css", :media "screen", :rel "stylesheet"}]
      [:style (index-css)]
      [:link {:href "/images/favicon.png", :rel "shortcut icon", :type "image/png"}]]
     [:body
      [:div.header
       [:div.home-menu.pure-menu.pure-menu-open.pure-menu-horizontal.pure-menu-fixed
        [:a.pure-menu-heading {:href ""} "systems-toolbox"]
        [:ul
         [:li [:div#jvm-stats-frame]]]]]
      [:div.splash-container
       [:div.splash
        [:h1.splash-head "Trailing Mouse Moves Example"]
        [:p.splash-subhead "WebSockets Latency Visualizer"]]]
      [:div.content-wrapper
       [:div.content
        [:h2.content-head.is-center "Systems Toolbox Example #1"]
        [:div.l-box-lrg
         [:p "This application is both a sample for building a single-page app communicating with the server over
              WebSockets and a visualizer for how well the WebSocket is performing. Please move your mouse in the
              white box below. The mouse move events will then be sent to the server and returned over the WebSocket
              connection and the time for sending and returning each message is measured. These times are then
              shown in histograms. Check out the code on "
          [:a {:href "https://github.com/matthiasn/systems-toolbox"} "GitHub"] "."]]]
       [:div.content [:div.l-box-lrg.pure-g
                      [:div.pure-u-1.pure-u-md-1-3 [:div#mouse]]
                      [:div.pure-u-1.pure-u-md-1-6 [:div]]
                      [:div.pure-u-1.pure-u-md-1-2 [:div#observer]]]]
       [:div.content [:div.l-box-lrg [:div#histograms]]]
       [:div.content
        [:div.l-box-lrg
         [:p "The histograms above are entirely drawn in ClojureScript - without any additional charting library.
              The number of bins is determined by applying the "
          [:a {:href "http://en.wikipedia.org/wiki/Freedman–Diaconis_rule"} "Freedman-Diaconis rule"] ".
              The first histogram takes the entire sample into account whereas the second only dispays the observations
              that fall within the 99th percentile in order to remove potential outliers."]]]
       [:div.content
        [:div.l-box-lrg.pure-g
         [:div#messages.pure-u-1.pure-u-md-1-2]
         [:div#snapshots.pure-u-1.pure-u-md-1-2]]]
       [:div.content
        [:div.l-box-lrg
         [:div#observer]]]]
      [:script {:src "/js/build/example.js"}]]]))

(def sente-map
  "Configuration map for sente-cmp."
  {:index-page-fn index-page
   :relay-types   #{:cmd/mouse-pos :stats/jvm}})
