(defproject matthiasn/redux-counter01 "0.6.1-SNAPSHOT"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.293"]
                 [hiccup "1.0.5"]
                 [clj-pid "0.1.2"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [org.clojure/tools.logging "0.3.1"]
                 [matthiasn/systemd-watchdog "0.1.2"]
                 [matthiasn/systems-toolbox "0.6.2"]
                 [matthiasn/systems-toolbox-sente "0.6.1"]
                 [matthiasn/systems-toolbox-ui "0.6.1"]]

  :source-paths ["src/clj/"]

  :clean-targets ^{:protect false} ["resources/public/js/build/" "target/"]

  :main example.core

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.8"]]

  :figwheel {:server-port 3452
             :css-dirs    ["resources/public/css"]}

  :profiles {:uberjar {:aot        :all
                       :auto-clean false}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs" "env/dev/cljs"]
     :figwheel     true
     :compiler     {:main          "example.dev"
                    :asset-path    "js/build"
                    :optimizations :none
                    :output-dir    "resources/public/js/build/"
                    :output-to     "resources/public/js/build/example.js"
                    :source-map    true}}
    {:id           "release"
     :source-paths ["src/cljs"]
     :compiler     {:main          "example.core"
                    :asset-path    "js/build"
                    :output-to     "resources/public/js/build/example.js"
                    :optimizations :advanced}}]})
