(defproject matthiasn/systems-toolbox "0.3.12"
  :description "Toolbox for building Systems in Clojure"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.ow2.asm/asm-all "5.0.4"]
                 [org.clojure/core.async "0.2.371"]
                 [reagent "0.5.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/sente "1.6.0"]
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [com.cognitect/transit-cljs "0.8.225"]
                 [compojure "1.4.0" :exclusions [commons-codec]]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [org.jboss.logging/jboss-logging "3.3.0.Final"]
                 [org.immutant/web "2.1.0" :exclusions [org.jboss.logging/jboss-logging org.slf4j/slf4j-api]]]

  :plugins [[lein-codox "0.9.0" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.1.1"]]

  :cljsbuild {:builds {:test
                       {:source-paths ["test"]
                        :compiler     {:output-to     "resources/test.js"
                                       :optimizations :advanced}
                        :jar          false}}})
