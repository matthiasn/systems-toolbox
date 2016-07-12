(defproject matthiasn/systems-toolbox "0.6.1-alpha2"
  :description "Toolbox for building Systems in Clojure"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/cljc"]

  :dependencies [[org.clojure/core.match "0.3.0-alpha4" :exclusions [org.clojure/tools.analyzer.jvm]]
                 [org.ow2.asm/asm-all "5.1"]
                 [org.clojure/core.async "0.2.385"]
                 [org.clojure/tools.logging "0.3.1"]
                 [io.aviso/pretty "0.1.27"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]]

  :plugins [[lein-codox "0.9.5"]
            [test2junit "1.2.2"]
            [lein-doo "0.1.7"]
            [com.jakemccrary/lein-test-refresh "0.16.0"]
            [lein-cljsbuild "1.1.3"]]

  :test2junit-output-dir ~(or (System/getenv "CIRCLE_TEST_REPORTS") "target/test2junit")

  :test-refresh {:notify-on-success false
                 :changes-only      false}

  :test-paths ["test"]
  ;:test-paths ["test" "perf"]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                                  [org.clojure/clojurescript "1.9.93"
                                   :exclusions [org.clojure/tools.reader]]
                                  [org.clojure/tools.logging "0.3.1"]
                                  [ch.qos.logback/logback-classic "1.1.7"]]}}

  :cljsbuild {:builds [{:id           "cljs-test"
                        :source-paths ["src" "test"]
                        :compiler     {:output-to     "out/testable.js"
                                       :main          matthiasn.systems-toolbox.runner
                                       :optimizations :advanced}}
                       {:id           "cljs-perf-test"
                        :source-paths ["src" "test"]
                        :compiler     {:output-to     "out/perf.js"
                                       :main          matthiasn.systems-toolbox.perf-runner
                                       :optimizations :advanced}}]})
