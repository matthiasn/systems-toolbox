(defproject matthiasn/systems-toolbox "0.1.14-SNAPSHOT"
  :description "Toolbox for building Systems in Clojure"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "target/generated/src/clj" "target/generated/src/cljs"]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                                  [org.clojure/clojurescript "0.0-3119"]
                                  [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                                  [reagent "0.5.0"]
                                  [com.taoensso/sente "1.4.1"]
                                  [org.clojure/core.match "0.3.0-alpha4"]
                                  [http-kit "2.1.19"]]
                   :plugins [[com.keminglabs/cljx "0.6.0" :exclusions [org.clojure/clojure]]
                             [codox "0.8.8"]
                             [lein-cljsbuild "1.0.5"]
                             [com.cemerick/clojurescript.test "0.3.1"]]
                   :cljx {:builds [{:source-paths ["src/cljx"]
                                    :output-path "target/generated/src/clj"
                                    :rules :clj}
                                   {:source-paths ["src/cljx"]
                                    :output-path "target/generated/src/cljs"
                                    :rules :cljs}]}}}

  :prep-tasks [["cljx" "once"] "javac" "compile"]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/src/clj"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/src/cljs"
                   :rules :cljs}]}

  :cljsbuild {:builds {:test
                       {:source-paths ["test"]
                        ;:dependencies [[org.clojure/clojurescript "0.0-3119"]]
                        :compiler {:output-to "resources/test.js"
                                   :optimizations :advanced}
                        :jar false}}})
