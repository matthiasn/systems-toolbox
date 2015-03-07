(ns com.matthiasnehlsen.systems-toolbox.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<! >! chan put!]]))

(defn component-with-channels
  "Creates a component with attached in-chan and out-chan.
   It takes a function fn and both an in- and an out-buffer.
   The function fn is called with the internal put-fn that puts
   messages onto the out-chan. Fn is expected to return another
   function that can then be called for each message on the
   in-chan. Component state can thus be kept inside the initial
   call to fn."
  [fn in-buffer out-buffer]
  (let [in-chan (chan in-buffer)
        out-chan (chan out-buffer)
        put-fn #(put! out-chan %)
        msg-fn (fn put-fn)]
    (go-loop [] 
             (msg-fn (<! in-chan))
             (recur))
    {:in-chan in-chan :out-chan out-chan}))
