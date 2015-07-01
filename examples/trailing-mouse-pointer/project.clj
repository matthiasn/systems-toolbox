(defproject matthiasn/trailing-mouse-pointer "0.1.0-SNAPSHOT"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-RC1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [hiccup "1.0.5"]
                 [hiccup-bridge "1.0.1"]
                 [garden "1.2.5"]
                 [clj-pid "0.1.1"]
                 [matthiasn/systems-toolbox "0.2.1-SNAPSHOT"]
;                 [reagent "0.5.0" :exclusions [cljsjs/react]]
                 [reagent "0.5.0"]
                 [incanter "1.5.6"]
                 [clj-time "0.9.0"]
                 [lein-figwheel "0.3.5"]
                 [org.clojure/clojurescript "0.0-3308"]]

  :source-paths ["src/clj/"]

  :clean-targets ^{:protect false} ["resources/public/js/build/"]

  :main example.core

  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.3.5"]
            [codox "0.8.10"]]

  :cljsbuild {:builds [{:id "release"
                        :source-paths ["src/cljs" "env/dev/cljs"]
                        :figwheel true
                        :compiler {:main "reagent-proj.dev"
                                   :asset-path "js/build"
                                   :optimizations :none
                                   ;:optimizations :simple
                                   ;:source-map "resources/public/js/build/example.js.map"
                                   :output-dir "resources/public/js/build/"
                                   :output-to "resources/public/js/build/example.js"}}]})
