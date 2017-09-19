(defproject matthiasn/systems-toolbox "0.6.16"
  :description "Toolbox for building Systems in Clojure"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/cljc"]

  :dependencies [[org.clojure/core.match "0.3.0-alpha4"
                  :exclusions [org.clojure/tools.analyzer.jvm]]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/tools.logging "0.4.0"]
                 [io.aviso/pretty "0.1.34"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]]

  :plugins [[lein-codox "0.10.3"]
            [test2junit "1.3.3"]
            [lein-doo "0.1.7"]
            [lein-cloverage "1.0.9"]
            [lein-ancient "0.6.10"]
            [com.jakemccrary/lein-test-refresh "0.21.1"]
            [lein-cljsbuild "1.1.7"]]

  :test2junit-output-dir
  ~(or (System/getenv "CIRCLE_TEST_REPORTS") "target/test2junit")

  :test-refresh {:notify-on-success false
                 :changes-only      false}

  :test-paths ["test"]
  ;:test-paths ["dev-resources" "test" "perf"]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0-beta1"]
                                  [org.clojure/clojurescript "1.9.908"]
                                  [org.clojure/tools.logging "0.4.0"]
                                  [ch.qos.logback/logback-classic "1.2.3"]]
                   :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}

  :cljsbuild
  {:builds [{:id           "cljs-test"
             :source-paths ["src" "test"]
             :compiler     {:output-to     "out/testable.js"
                            :main          matthiasn.systems-toolbox.runner
                            :optimizations :advanced}}
            {:id           "cljs-perf-test"
             :source-paths ["perf" "src" "test"]
             :compiler     {:output-to     "out/perf.js"
                            :main          matthiasn.systems-toolbox.perf-runner
                            :optimizations :advanced}}]})
