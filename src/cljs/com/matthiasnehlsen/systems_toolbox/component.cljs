(ns com.matthiasnehlsen.systems-toolbox.component
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :refer [<! >! chan put!]]))

(defn component-with-channels
  "Create a component with attached in-chan and out-chan. Inside, there's a go-loop
   that takes a message off the in-chan, calls fn with it and puts the result back
   onto the out-chan."
  [fn in-buffer out-buffer]
  (let [in-chan (chan in-buffer)
        out-chan (chan out-buffer)
        put-fn #(put! out-chan %)
        msg-fn (fn put-fn)]
    (go-loop []
             (let [msg (<! in-chan)]
               (msg-fn msg)
               (recur)))
    {:in-chan in-chan :out-chan out-chan}))
