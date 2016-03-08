(defproject matthiasn/redux-counter01 "0.5.1-SNAPSHOT"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [hiccup "1.0.5"]
                 [clj-pid "0.1.2"]
                 [ch.qos.logback/logback-classic "1.1.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [matthiasn/systems-toolbox "0.5.14"]
                 [matthiasn/systems-toolbox-sente "0.5.12"]
                 [matthiasn/systems-toolbox-ui "0.5.6"]]

  :source-paths ["src/clj/"]

  :clean-targets ^{:protect false} ["resources/public/js/build/" "target/"]

  :main example.core

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.0-6"]]

  :figwheel {:server-port 3450
             :css-dirs    ["resources/public/css"]}

  :cljsbuild {:builds [{:id           "dev"
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
                        :figwheel     true
                        :compiler     {:main          "example.core"
                                       :asset-path    "js/build"
                                       :output-to     "resources/public/js/build/example.js"
                                       :optimizations :advanced}}]})
