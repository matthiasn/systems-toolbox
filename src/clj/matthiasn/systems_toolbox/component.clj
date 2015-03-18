(ns matthiasn.systems-toolbox.component
  (:require [cljs.core.match :refer-macros [match]]
            [cljs.core.async :refer [<! >! chan put! sub pipe mult pub go-loop buffer sliding-buffer dropping-buffer timeout]]))

(defn make-chan-w-buf
  "Create a channel with a buffer of the specified size and type."
  [config]
  (match config
         [:sliding  n]  (chan (sliding-buffer n))
         [:dropping n]  (chan (dropping-buffer n))
         [:buffer   n]  (chan (buffer n))
         :else          (prn "invalid: " config)))

(def component-defaults
  {:in-chan  [:buffer 1]  :sliding-in-chan  [:sliding 1]  :throttle-ms 5
   :out-chan [:buffer 1]  :sliding-out-chan [:sliding 1]  :atom true})

(defn msg-handler-loop
  "Constructs a map with a channel for the provided channel keyword, with the buffer
  configured according to cfg for the channel keyword. Then starts loop for taking messages
  off the returned channel and calling the provided handler-fn with the msg.
  Does not process return values from the processing step; instead, put-fn needs to be
  called to produce output."
  [state handler-fn put-fn cfg chan-key]
  (when handler-fn
    (let [chan (make-chan-w-buf (chan-key cfg))]
      (go-loop []
               (handler-fn state put-fn (<! chan))
               (when (= chan-key :sliding-in-chan) (<! (timeout (:throttle-ms cfg))))
               (recur))
      {chan-key chan})))

(defn make-component
  "Creates a component with attached in-chan, out-chan, sliding-in-chan and sliding-out-chan.
  It takes the initial state atom, the handler function for messages on in-chan, and the
  sliding-handler function, which handles messages on sliding-in-chan.
  By default, in-chan and out-chan have standard buffers of size one, whereas sliding-in-chan
  and sliding-out-chan have sliding buffers of size one.
  The buffer sizes can be configured.
  The sliding-channels are meant for events where only ever the latest version is of interest,
  such as mouse moves or published state snapshots in the case of UI components rendering
  state snapshots from other components."
  ([mk-state handler sliding-handler]
   (make-component mk-state handler sliding-handler component-defaults))
  ([mk-state handler sliding-handler opts]
   (let [cfg (merge component-defaults opts)
         out-chan (make-chan-w-buf (:out-chan cfg))
         sliding-out-chan (make-chan-w-buf (:sliding-out-chan cfg))
         put-fn #(put! out-chan %)
         out-mult (mult out-chan)
         state (mk-state put-fn)]
     (when (:atom cfg)  ; not the case in sente / ws component - ws map has an atom inside
       (add-watch state :watcher (fn [_ _ _ new-state] (put! sliding-out-chan [:app-state new-state]))))
     (merge
       {:out-mult out-mult
        :state-pub (pub sliding-out-chan first)}
       (msg-handler-loop state handler put-fn cfg :in-chan)
       (msg-handler-loop state sliding-handler put-fn cfg :sliding-in-chan)))))