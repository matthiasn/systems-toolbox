(ns ^:figwheel-no-load example.dev
  (:require [example.core :as c]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(defn jscb [] 
  (c/init!))

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3451/figwheel-ws"
  :jsload-callback jscb)
