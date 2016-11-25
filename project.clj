(defproject matthiasn/systems-toolbox "0.5.23"
  :description "Toolbox for building Systems in Clojure"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/cljc"]

  :dependencies [[org.clojure/core.match "0.3.0-alpha4" :exclusions [org.clojure/tools.analyzer.jvm]]
                 [org.ow2.asm/asm-all "5.1"]
                 [org.clojure/core.async "0.2.395"]
                 [org.clojure/tools.logging "0.3.1"]
                 [io.aviso/pretty "0.1.30"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]]

  :plugins [[lein-codox "0.10.2"]
            [test2junit "1.2.5"]
            [lein-doo "0.1.7"]
            [lein-cljsbuild "1.1.4"]]

  :test2junit-output-dir ~(or (System/getenv "CIRCLE_TEST_REPORTS") "target/test2junit")

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/clojurescript "1.9.293"]
                                  [org.clojure/tools.logging "0.3.1"]
                                  [ch.qos.logback/logback-classic "1.1.7" :exclusions [org.slf4j/slf4j-api]]
                                  [org.slf4j/jul-to-slf4j "1.7.21"]
                                  [org.slf4j/jcl-over-slf4j "1.7.21"]
                                  [org.slf4j/log4j-over-slf4j "1.7.21"]]}}

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
