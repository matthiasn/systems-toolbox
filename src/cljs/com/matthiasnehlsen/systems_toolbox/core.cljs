(ns com.matthiasnehlsen.systems-toolbox.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.match :refer-macros [match]]
            [cljs.core.async :refer [<! >! chan put! buffer sliding-buffer dropping-buffer]]))

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

(defn make-chan-w-buf
  [config]
  (match config
         [:sliding n]  (chan (sliding-buffer n))
         [:dropping n] (chan (dropping-buffer n))
         [:buffer n]   (chan (buffer n))
         :else (prn "invalid: " config)))

(defn make-chans-w-buf
  [channels]
  (into {} (map (fn [[k v]] [k (make-chan-w-buf v)]) channels)))

(defn component-single-in-mult-out
  "Creates a component with attached in-chan and out-chan.
   It takes a function fn and both an in- and an out-buffer.
   The function fn is called with the internal put-fn that puts
   messages onto the out-chan. Fn is expected to return another
   function that can then be called for each message on the
   in-chan. Component state can thus be kept inside the initial
   call to fn."
  [fn channel-config out-chan-selector]
  (let [in-chan (make-chan-w-buf (:in-chan channel-config))
        out-chans (make-chans-w-buf (:out-chans channel-config))
        put-fn #(put! ((out-chan-selector %) out-chans) %)
        msg-fn (fn put-fn)]
    (go-loop []
             (msg-fn (<! in-chan))
             (recur))
    {:in-chan in-chan :out-chans out-chans}))

(defn component-single-in-single-out
  "Creates a component with attached in-chan and out-chan.
   It takes a function fn and both an in- and an out-buffer.
   The function fn is called with the internal put-fn that puts
   messages onto the out-chan. Fn is expected to return another
   function that can then be called for each message on the
   in-chan. Component state can thus be kept inside the initial
   call to fn."
  [fn channel-config]
  (let [in-chan (make-chan-w-buf (:in-chan channel-config))
        out-chan (make-chan-w-buf (:out-chan channel-config))
        put-fn #(put! out-chan %)
        msg-fn (fn put-fn)]
    (go-loop []
             (msg-fn (<! in-chan))
             (recur))
    {:in-chan in-chan :out-chan out-chan}))
