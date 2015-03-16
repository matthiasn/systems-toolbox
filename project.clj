(defproject matthiasn/systems-toolbox "0.1.8-SNAPSHOT"
  :description "Toolbox for building Systems in Clojure"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3119"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [reagent "0.5.0-alpha3"]
                 [com.taoensso/sente "1.4.1"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [http-kit "2.1.19"]]
  :source-paths ["src/cljs"]
  :plugins [[lein-cljsbuild "1.0.5"]]
  :cljsbuild {:builds {:test
                       {:source-paths ["test"]
                        :dependencies [[org.clojure/clojurescript "0.0-3119"]]
                        :compiler {:output-to "resources/test.js"
                                   :optimizations :advanced}
                        :jar false}}})
