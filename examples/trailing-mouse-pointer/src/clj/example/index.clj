(ns example.index
  (:gen-class)
  (:require
    [hiccup.core :refer [html]]
    [garden.core :refer [css]]
    [garden.units :as u :refer [px]]))

(defn index-css
  "Generate index page CSS in Clojure using Garden."
  []
  (css {:pretty-print? false}
       [:.content {:background-color "#EEE"}]
       [:a {:font-weight :bold
            :text-decoration :none
            :color "#1f8dd6"}]
       [:p {:line-height (px 25)
            :margin-left "10%" :margin-right "10%"}]
       [:#mouse {:border-color :darkgray
                 :border-width (px 1)
                 :border-style :solid}]))

(defn index-page
  "Generates index page HTML with the specified page title."
  [dev?]
  (html
    [:html
     {:lang "en"}
     [:head
      [:meta {:content "width=device-width, initial-scale=1", :name "viewport"}]
      [:title "Systems-Toolbox: Trailing Mouse Pointer Example"]
      [:link {:href "/bower_components/pure/pure-min.css", :media "screen", :rel "stylesheet"}]
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
              shown in a histogram. Check out the code on "
          [:a {:href "https://github.com/matthiasn/systems-toolbox"} "GitHub"] "."]]]
       [:div.content
        [:div.pure-g [:div.l-box-lrg [:div#mouse.pure-u-1]]]]]
      [:script {:src "/js/build/example.js"}]]]))
