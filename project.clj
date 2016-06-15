(defproject matthiasn/systems-toolbox "0.6.1-SNAPSHOT"
  :description "Toolbox for building Systems in Clojure"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/cljc"]

  :dependencies [[org.clojure/tools.reader "1.0.0-beta1"]
                 [org.clojure/core.match "0.3.0-alpha4" :exclusions [org.clojure/tools.analyzer.jvm]]
                 [org.ow2.asm/asm-all "5.1"]
                 [org.clojure/core.async "0.2.382"]
                 [org.clojure/tools.logging "0.3.1"]
                 [io.aviso/pretty "0.1.26"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]]

  :plugins [[lein-codox "0.9.5"]
            [test2junit "1.2.2"]
            [lein-doo "0.1.6"]
            [com.jakemccrary/lein-test-refresh "0.15.0"]
            [lein-cljsbuild "1.1.3"]]

  :test2junit-output-dir ~(or (System/getenv "CIRCLE_TEST_REPORTS") "target/test2junit")

  :test-refresh {:notify-on-success false
                 :changes-only      false}

  :test-paths ["test"]
  ;:test-paths ["test" "perf"]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0-alpha6"]
                                  [org.clojure/clojurescript "1.9.36"]
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
