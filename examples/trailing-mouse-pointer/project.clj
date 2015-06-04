(defproject matthiasn/trailing-mouse-pointer "0.1.0-SNAPSHOT"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-beta2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [hiccup "1.0.5"]
                 [hiccup-bridge "1.0.1"]
                 [garden "1.2.5"]
                 [clj-pid "0.1.1"]
                 [matthiasn/systems-toolbox "0.1.33-SNAPSHOT"
                  :exclusions [reagent org.clojure/clojure org.clojure/clojurescript]]
;                 [reagent "0.5.0" :exclusions [cljsjs/react]]
                 [reagent "0.5.0"]
                 [incanter "1.5.6"]
                 [clj-time "0.9.0"]
                 [org.clojure/clojurescript "0.0-3269"
                  :classifier "aot" :exclusions [org.clojure/tools.reader org.clojure/data.json]]
                 [org.clojure/tools.reader "0.9.2" :classifier "aot"]
                 [org.clojure/data.json "0.2.6" :classifier "aot"]]

  :source-paths ["src/clj/"]

  :clean-targets ^{:protect false} ["resources/public/js/build/"]

  :main example.core

  :plugins [[lein-cljsbuild "1.0.5"]
            [codox "0.8.10"]]

  :cljsbuild {:builds [{:id "release"
                        :source-paths ["src/cljs"]
                        :compiler {:output-dir "resources/public/js/build/"
                                   :output-to "resources/public/js/build/example.js"
                                   ; :optimizations :advanced
                                   :optimizations :simple
                                   ;:source-map "resources/public/js/build/example.js.map"
                                   }}]})
