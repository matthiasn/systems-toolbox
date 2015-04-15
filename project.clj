(defproject matthiasn/systems-toolbox "0.1.28-SNAPSHOT"
  :description "Toolbox for building Systems in Clojure"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "target/generated/src/clj" "target/generated/src/cljs"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3196"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [reagent "0.5.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/sente "1.4.1"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [com.cognitect/transit-clj "0.8.271"]
                 [com.cognitect/transit-cljs "0.8.207"]
                 [compojure "1.3.3"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.4"]
                 [http-kit "2.1.19"]]

  :plugins [[com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]
            [codox "0.8.8"]
            [lein-cljsbuild "1.0.5"]
            [com.cemerick/clojurescript.test "0.3.1"]]

  :prep-tasks [["cljx" "once"] "javac" "compile"]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/src/clj"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/src/cljs"
                   :rules :cljs}]}

  :cljsbuild {:builds {:test
                       {:source-paths ["test"]
                        :compiler {:output-to "resources/test.js"
                                   :optimizations :advanced}
                        :jar false}}})
