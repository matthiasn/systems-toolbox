(ns matthiasn.systems-toolbox.component
  #+clj (:gen-class)
  #+cljs (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    #+clj [clojure.core.match :refer [match]]
    #+cljs [cljs.core.match :refer-macros [match]]
    #+cljs [matthiasn.systems-toolbox.helpers :refer [request-animation-frame]]
    #+clj [clojure.core.async :refer [<! >! chan put! sub pipe mult tap pub buffer sliding-buffer dropping-buffer go-loop timeout]]
    #+cljs [cljs.core.async :refer [<! >! chan put! sub pipe mult tap pub buffer sliding-buffer dropping-buffer timeout]]
    #+clj [clojure.tools.logging :as log]))

#+clj (defn now [] (System/currentTimeMillis))
#+cljs (defn now [] (.getTime (js/Date.)))

(defn make-chan-w-buf
  "Create a channel with a buffer of the specified size and type."
  [config]
  (match config
         [:sliding n] (chan (sliding-buffer n))
         [:buffer n] (chan (buffer n))
         :else (prn "invalid: " config)))

(def component-defaults
  {:in-chan  [:buffer 1] :sliding-in-chan [:sliding 1] :throttle-ms 1
   :out-chan [:buffer 1] :sliding-out-chan [:sliding 1] :firehose-chan [:buffer 1]})

(defn msg-handler-loop
  "Constructs a map with a channel for the provided channel keyword, with the buffer
  configured according to cfg for the channel keyword. Then starts loop for taking messages
  off the returned channel and calling the provided handler-fn with the msg.
  Does not process return values from the processing step; instead, put-fn needs to be
  called to produce output."
  [state handler-fn put-fn cfg cmp-id chan-key firehose-chan]
  (when handler-fn
    (let [chan (make-chan-w-buf (chan-key cfg))]
      (go-loop []
        (let [msg (<! chan)
              msg-meta (-> (merge (meta msg) {})
                           (assoc-in, [:cmp-seq] cmp-id)    ; TODO: replace by actual sequence
                           (assoc-in, [cmp-id :in-timestamp] (now)))
              [msg-type _] msg]
          (when-not (= "firehose" (namespace msg-type))
            (put! firehose-chan [:firehose/cmp-recv {:cmp-id cmp-id :msg msg}]))
          (if (= msg-type :cmd/get-state)
            (put-fn [:state/snapshot {:cmp-id cmp-id :snapshot @state}])
            (handler-fn state put-fn (with-meta msg msg-meta)))
          (when (= chan-key :sliding-in-chan) (<! (timeout (:throttle-ms cfg))))
          (recur)))
      {chan-key chan})))

(defn msg-handler-loop2
  "Constructs a map with a channel for the provided channel keyword, with the buffer
  configured according to cfg for the channel keyword. Then starts loop for taking messages
  off the returned channel and calling the provided handler-fn with the msg.
  Does not process return values from the processing step; instead, put-fn needs to be
  called to produce output."
  [{:keys [cmp-state handler-map put-fn cfg cmp-id firehose-chan] :as cmp-map} chan-key]
  (when handler-map
    (let [chan (make-chan-w-buf (chan-key cfg))]
      (go-loop []
        (let [msg (<! chan)
              msg-meta (-> (merge (meta msg) {})
                           (assoc-in, [:cmp-seq] cmp-id)    ; TODO: replace by actual sequence
                           (assoc-in, [cmp-id :in-timestamp] (now)))
              [msg-type msg-payload] msg
              handler-fn (msg-type handler-map)]
          (when-not (= "firehose" (namespace msg-type))
            (put! firehose-chan [:firehose/cmp-recv {:cmp-id cmp-id :msg msg}]))
          (if (= msg-type :cmd/get-state)
            (put-fn [:state/snapshot {:cmp-id cmp-id :snapshot @cmp-state}])
            (when handler-fn (handler-fn (merge cmp-map {:msg         (with-meta msg msg-meta)
                                                         :msg-type    msg-type
                                                         :msg-meta    msg-meta
                                                         :msg-payload msg-payload}))))
          (when (= chan-key :sliding-in-chan) (<! (timeout (:throttle-ms cfg))))
          (recur)))
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
  [{:keys [cmp-id state-fn handler handler-map state-pub-handler opts]}]
  (let [cfg (merge component-defaults opts)
        out-chan (make-chan-w-buf (:out-chan cfg))
        firehose-chan (make-chan-w-buf (:firehose-chan cfg))
        out-pub-chan (make-chan-w-buf (:out-chan cfg))
        sliding-out-chan (make-chan-w-buf (:sliding-out-chan cfg))
        put-fn (fn [msg]
                 (let [msg-meta (-> (merge (meta msg) {})
                                    (assoc-in, [:cmp-seq] cmp-id) ; TODO: replace by actual sequence
                                    (assoc-in, [cmp-id :out-timestamp] (now)))
                       msg-w-meta (with-meta msg msg-meta)]
                   (put! out-chan msg-w-meta)
                   (put! firehose-chan [:firehose/cmp-put {:cmp-id cmp-id :msg msg-w-meta}])))
        out-mult (mult out-chan)
        firehose-mult (mult firehose-chan)
        state (state-fn put-fn)
        watch-state (if-let [watch (:watch cfg)] (watch state) state)
        changed (atom true)
        cmp-map {:out-mult          out-mult
                 :firehose-chan     firehose-chan
                 :firehose-mult     firehose-mult
                 :out-pub           (pub out-pub-chan first)
                 :state-pub         (pub sliding-out-chan first)
                 :cmp-id            cmp-id
                 :handler-map       handler-map
                 :cmp-state         state
                 :put-fn            put-fn
                 :cfg               cfg
                 :state-snapshot-fn (fn [] @watch-state)}]
    (tap out-mult out-pub-chan)

    #+clj
    (try
      (add-watch watch-state
                 :watcher
                 (fn [_ _ _ new-state]
                   (put! sliding-out-chan (with-meta [:app-state new-state] {:from cmp-id}))))
      (catch Exception e (log/error cmp-id "Exception attempting to watch atom:" watch-state e)))

    #+cljs
    (letfn [(step []
                  (request-animation-frame step)
                  (when @changed
                    (put! sliding-out-chan (with-meta [:app-state @watch-state] {:from cmp-id}))
                    (swap! changed not)))]
      (request-animation-frame step)
      (try
        (add-watch watch-state
                   :watcher
                   (fn [_ _ _ new-state]
                     (reset! changed true)))
        (catch js/Object e (prn cmp-id " Exception attempting to watch atom: " watch-state e))))

    (merge cmp-map
           (if handler-map
             (msg-handler-loop2 cmp-map :in-chan)
             (msg-handler-loop state handler put-fn cfg cmp-id :in-chan firehose-chan))
           (msg-handler-loop state state-pub-handler put-fn cfg cmp-id :sliding-in-chan firehose-chan))))
