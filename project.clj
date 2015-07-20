(defproject matthiasn/systems-toolbox "0.2.16"
  :description "Toolbox for building Systems in Clojure"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.ow2.asm/asm-all "4.2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha" :exclusions [org.ow2.asm/asm-all]]
                 [reagent "0.5.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/sente "1.5.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [com.cognitect/transit-clj "0.8.275"]
                 [com.cognitect/transit-cljs "0.8.220"]
                 [compojure "1.3.4" :exclusions [commons-codec]]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.5"]
                 [matthiasn/http-kit "2.1.19"]]

  :plugins [[codox "0.8.8"]
            [lein-cljsbuild "1.0.6"]
            [com.cemerick/clojurescript.test "0.3.1"]]

  :cljsbuild {:builds {:test
                       {:source-paths ["test"]
                        :compiler {:output-to "resources/test.js"
                                   :optimizations :advanced}
                        :jar false}}})
