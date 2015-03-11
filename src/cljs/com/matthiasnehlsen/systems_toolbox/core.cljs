(ns com.matthiasnehlsen.systems-toolbox.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.match :refer-macros [match]]
            [cljs.core.async :refer [<! >! chan put! pub buffer sliding-buffer dropping-buffer timeout]]))

(defn make-chan-w-buf
  "Create a channel with a buffer of the specified size and type."
  [config]
  (match config
         [:sliding n]  (chan (sliding-buffer n))
         [:dropping n] (chan (dropping-buffer n))
         [:buffer n]   (chan (buffer n))
         :else (prn "invalid: " config)))

(defn make-chans-w-buf
  "Create a channels with buffers of the specified size and type."
  [channels]
  (into {} (map (fn [[k v]] [k (make-chan-w-buf v)]) channels)))

(defn single-in-single-out
  "Creates a component with attached in-chan and out-chan.
   It takes a function fn and both an in- and an out-buffer.
   The function fn is called with the internal put-fn that puts
   messages onto the out-chan. Fn is expected to return another
   function that can then be called for each message on the
   in-chan. Component state can thus be kept inside the initial
   call to fn."
  ([fn] (single-in-single-out fn {:in-chan [:buffer 1] :out-chan [:buffer 1]}))
  ([fn channel-config]
   (let [in-chan (make-chan-w-buf (:in-chan channel-config))
         out-chan (make-chan-w-buf (:out-chan channel-config))
         put-fn #(put! out-chan %)
         msg-fn (fn put-fn)]
     (go-loop []
              (msg-fn (<! in-chan))
              (when-let [t (:in-timeout channel-config)] (<! (timeout t)))
              (recur))
     {:in-chan in-chan :out-chan out-chan})))

(def component-defaults {:in-chan [:buffer 1]
                         :out-chan [:buffer 1]
                         :sliding-in-chan [:sliding 1]
                         :sliding-out-chan [:sliding 1]
                         :sliding-in-timeout 5})

(defn make-component
  "Creates a component with attached in-chan, out-chan, sliding-in-chan
  and sliding-out-chan.
  It takes the initial state atom, the handler function for messages on
  in-chan, and the sliding-handler function, which handles messages on
  sliding-in-chan.
  By default, in-chan and out-chan have standard buffers of size one,
  whereas sliding-in-chan and sliding-out-chan have sliding buffers of
  size one. The buffer sizes can be configured.
  The sliding-channels are meant for events where only ever the latest
  version is of interest, such as mouse moves or published state
  snapshots in the case of UI components rendering state snapshots from
  other components."
  ([mk-state handler sliding-handler]
   (make-component mk-state handler sliding-handler component-defaults))
  ([mk-state handler sliding-handler opts]
   (let [cfg (merge component-defaults opts)
         out-chan (make-chan-w-buf (:out-chan cfg))
         sliding-out-chan (make-chan-w-buf (:sliding-out-chan cfg))
         put-fn #(put! out-chan %)
         state (mk-state put-fn)]
     (add-watch state :watcher (fn [_ _ _ new-state] (put! sliding-out-chan [:app-state new-state])))
     (merge
       {:out-chan out-chan
        :state-pub (pub sliding-out-chan first)}
       (when handler
         (let [in-chan (make-chan-w-buf (:in-chan cfg))]
           (go-loop []
                    (handler state put-fn (<! in-chan))
                    (recur))
           {:in-chan in-chan}))
       (when sliding-handler
         (let [sliding-in-chan (make-chan-w-buf (:sliding-in-chan cfg))]
           (go-loop []
                    (sliding-handler state put-fn (<! sliding-in-chan))
                    (recur))
           {:sliding-in-chan sliding-in-chan}))))))
