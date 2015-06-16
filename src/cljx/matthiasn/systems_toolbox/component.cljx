(ns matthiasn.systems-toolbox.component
  #+clj (:gen-class)
  #+cljs (:require-macros [cljs.core.async.macros :as cam :refer [go-loop]])
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
   :out-chan [:buffer 1] :sliding-out-chan [:sliding 1] :firehose-chan [:buffer 1]
   :snapshots-on-firehose true})

(defn msg-handler-loop
  "Constructs a map with a channel for the provided channel keyword, with the buffer
  configured according to cfg for the channel keyword. Then starts loop for taking messages
  off the returned channel and calling the provided handler-fn with the msg.
  Does not process return values from the processing step; instead, put-fn needs to be
  called to produce output."
  [{:keys [handler-map all-msgs-handler state-pub-handler put-fn cfg cmp-id firehose-chan] :as cmp-map} cmp-state chan-key]
  (let [chan (make-chan-w-buf (chan-key cfg))]
    (try
      (go-loop []
        (let [msg (<! chan)
              msg-meta (-> (merge (meta msg) {})
                           (assoc-in, [:cmp-seq] cmp-id)    ; TODO: replace by actual sequence
                           (assoc-in, [cmp-id :in-timestamp] (now)))
              [msg-type msg-payload] msg
              handler-fn (msg-type handler-map)
              msg-map (merge cmp-map {:msg         (with-meta msg msg-meta)
                                      :msg-type    msg-type
                                      :msg-meta    msg-meta
                                      :msg-payload msg-payload
                                      :cmp-state   cmp-state})]
          (when (= chan-key :sliding-in-chan)
            (state-pub-handler msg-map)
            (when-not (= "firehose" (namespace msg-type))
              (put! firehose-chan [:firehose/cmp-recv-state {:cmp-id cmp-id :msg msg}]))
            (<! (timeout (:throttle-ms cfg))))
          (when (= chan-key :in-chan)
            (when-not (= "firehose" (namespace msg-type))
              (put! firehose-chan [:firehose/cmp-recv {:cmp-id cmp-id :msg msg}]))
            (when (= msg-type :cmd/get-state) (put-fn [:state/snapshot {:cmp-id cmp-id :snapshot @cmp-state}]))
            (when handler-fn (handler-fn msg-map))
            (when all-msgs-handler (all-msgs-handler msg-map)))
          (recur)))
     #+clj (catch Exception e (prn cmp-id "Exception in msg-handler-loop: " e cmp-map))
     #+cljs (catch js/Object e (prn cmp-id "Exception in msg-handler-loop: " e cmp-map)))
    {chan-key chan}))

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
  [{:keys [cmp-id state-fn state-pub-handler opts] :as cmp-conf}]
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
        state (if state-fn (state-fn put-fn) (atom {}))
        watch-state (if-let [watch (:watch cfg)] (watch state) state)
        changed (atom true)
        cmp-map (merge cmp-conf {:out-mult          out-mult
                                 :firehose-chan     firehose-chan
                                 :firehose-mult     firehose-mult
                                 :out-pub           (pub out-pub-chan first)
                                 :state-pub         (pub sliding-out-chan first)
                                 :put-fn            put-fn
                                 :cfg               cfg
                                 :state-snapshot-fn (fn [] @watch-state)})]
    (tap out-mult out-pub-chan)
    #+clj (try
            (add-watch watch-state
                       :watcher
                       (fn [_ _ _ new-state]
                         (let [snapshot-msg (with-meta [:app-state new-state] {:from cmp-id})]
                           (put! sliding-out-chan snapshot-msg)
                           (when (:snapshots-on-firehose cfg)
                             (put! firehose-chan [:firehose/cmp-publish-state {:cmp-id cmp-id :msg snapshot-msg}])))))
            (catch Exception e (log/error cmp-id "Exception attempting to watch atom:" watch-state e)))
    #+cljs (letfn [(step []
                         (request-animation-frame step)
                         (when @changed
                           (let [snapshot-msg (with-meta [:app-state @watch-state] {:from cmp-id})]
                             (put! sliding-out-chan snapshot-msg)
                             (when (:snapshots-on-firehose cfg)
                               (put! firehose-chan [:firehose/cmp-publish-state {:cmp-id cmp-id :msg snapshot-msg}])))
                           (swap! changed not)))]
             (request-animation-frame step)
             (try (add-watch watch-state :watcher (fn [_ _ _ new-state] (reset! changed true)))
                  (catch js/Object e (prn cmp-id " Exception attempting to watch atom: " watch-state e))))
    (merge cmp-map
           (msg-handler-loop cmp-map state :in-chan)
           (msg-handler-loop cmp-map state :sliding-in-chan))))
