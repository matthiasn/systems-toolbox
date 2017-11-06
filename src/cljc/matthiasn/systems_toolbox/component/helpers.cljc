(ns matthiasn.systems-toolbox.component.helpers
  (:require
    #?(:clj  [clojure.pprint :as pp]
       :cljs [cljs.pprint :as pp])
    #?(:cljs [cljs-uuid-utils.core :as uuid])))

(defn now
  "Get milliseconds since epoch."
  []
  #?(:clj  (System/currentTimeMillis)
     :cljs (.getTime (js/Date.))))

(defn pp-str [data] (with-out-str (pp/pprint data)))

(defn make-uuid
  "Get a random UUID."
  []
  #?(:clj  (java.util.UUID/randomUUID)
     :cljs (uuid/make-random-uuid)))

#?(:cljs (def request-animation-frame
           (or (when (exists? js/window)
                 (or (.-requestAnimationFrame js/window)
                     (.-webkitRequestAnimationFrame js/window)
                     (.-mozRequestAnimationFrame js/window)
                     (.-msRequestAnimationFrame js/window)))
               (fn [callback] (js/setTimeout callback 17)))))
