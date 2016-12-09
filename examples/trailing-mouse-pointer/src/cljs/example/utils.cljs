(ns example.utils
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [chan sliding-buffer timeout put! <!]]))

(defn throttle [f time]
  (let [c (chan (sliding-buffer 1))]
    (go-loop []
             (apply f (<! c))
             (<! (timeout time))
             (recur))
    (fn [& args]
      (put! c (or args [])))))