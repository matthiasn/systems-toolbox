(ns matthiasn.systems-toolbox.component
  #+clj (:gen-class)
  #+cljs (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    #+clj [clojure.core.match :refer [match]]
    #+cljs [cljs.core.match :refer-macros [match]]
    #+clj [clojure.core.async :refer [<! >! chan put! sub pipe mult tap pub buffer sliding-buffer dropping-buffer go-loop timeout]]
    #+cljs [cljs.core.async :refer [<! >! chan put! sub pipe mult tap pub buffer sliding-buffer dropping-buffer timeout]]))

(defn make-chan-w-buf
  "Create a channel with a buffer of the specified size and type."
  [config]
  (match config
         [:sliding  n]  (chan (sliding-buffer n))
         [:buffer   n]  (chan (buffer n))
         :else          (prn "invalid: " config)))

(def component-defaults
  {:in-chan  [:buffer 1]  :sliding-in-chan  [:sliding 1]  :throttle-ms 5
   :out-chan [:buffer 1]  :sliding-out-chan [:sliding 1]})

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
  ([cmp-id mk-state handler sliding-handler]
   (make-component cmp-id mk-state handler sliding-handler component-defaults))

  ([cmp-id mk-state handler sliding-handler opts]
   (let [cfg (merge component-defaults opts)
         out-chan (make-chan-w-buf (:out-chan cfg))
         out-pub-chan (make-chan-w-buf (:out-chan cfg))
         sliding-out-chan (make-chan-w-buf (:sliding-out-chan cfg))
         put-fn (fn [msg]
                  (let [msg-meta (merge (meta msg) {:from cmp-id})]
                    (put! out-chan (with-meta msg msg-meta))))
         out-mult (mult out-chan)
         state (mk-state put-fn)]
     (tap out-mult out-pub-chan)

     #+clj
     (try
       (add-watch state
                  :watcher
                  (fn [_ _ _ new-state]
                    (put! sliding-out-chan (with-meta [:app-state new-state] {:from cmp-id}))))
       (catch Exception _ ()))

     #+cljs
     (try
       (add-watch state
                  :watcher
                  (fn [_ _ _ new-state]
                    (put! sliding-out-chan (with-meta [:app-state new-state] {:from cmp-id}))))
       (catch js/Object _ ()))

     (when-let [watch (:watch cfg)]
       (add-watch (watch state)
                  :watcher
                  (fn [_ _ _ new-state]
                    (put! sliding-out-chan (with-meta [:app-state new-state] {:from cmp-id})))))

     (merge
       {:out-mult out-mult
        :out-pub (pub out-pub-chan first)
        :state-pub (pub sliding-out-chan first)
        :cmp-id cmp-id}
       (msg-handler-loop state handler put-fn cfg :in-chan)
       (msg-handler-loop state sliding-handler put-fn cfg :sliding-in-chan)))))