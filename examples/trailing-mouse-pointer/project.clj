(defproject matthiasn/trailing-mouse-pointer "0.6.1-SNAPSHOT"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]
                 [org.clojure/core.async "0.6.532"]
                 [org.clojure/tools.logging "0.5.0"]
                 [org.clojure/tools.namespace "0.3.1"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [matthiasn/systemd-watchdog "0.1.3"]
                 [hiccup "1.0.5"]
                 [clj-pid "0.1.2"]
                 [matthiasn/systems-toolbox "0.6.41"]
                 [matthiasn/systems-toolbox-sente "0.6.32"]
                 [matthiasn/systems-toolbox-kafka "0.6.18"]
                 [re-frame "0.10.9"]
                 [clj-time "0.15.2"]]

  :source-paths ["src/cljc/" "src/clj/"]

  :clean-targets ^{:protect false} ["resources/public/js/build/" "target/"]

  :main example.core

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.19"]]

  :figwheel {:server-port 3451
             :css-dirs    ["resources/public/css"]}

  :profiles {:uberjar {:aot        :all
                       :auto-clean false}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljc/" "src/cljs" "env/dev/cljs"]
     :figwheel     true
     :compiler     {:main          "example.dev"
                    :asset-path    "js/build"
                    :optimizations :none
                    :output-dir    "resources/public/js/build/"
                    :output-to     "resources/public/js/build/example.js"
                    :source-map    true}}
    {:id           "release"
     :source-paths ["src/cljc/" "src/cljs"]
     :compiler     {:main          "example.core"
                    :asset-path    "js/build"
                    :output-to     "resources/public/js/build/example.js"
                    :optimizations :advanced}}]})
