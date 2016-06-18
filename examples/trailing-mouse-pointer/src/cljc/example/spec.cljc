(ns example.spec
  (:require
    #?(:clj  [clojure.spec :as s]
       :cljs [cljs.spec :as s])))

(s/def :cmd/mouse-pos
  (s/keys :req-un [:ex/x :ex/y]))
