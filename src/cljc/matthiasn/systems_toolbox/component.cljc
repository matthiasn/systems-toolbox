(ns matthiasn.systems-toolbox.component
  #?(:clj (:gen-class))
  #?(:cljs (:require-macros [cljs.core.async.macros :as cam :refer [go-loop]]))
  (:require
    #?(:clj  [clojure.core.match :refer [match]]
       :cljs [cljs.core.match :refer-macros [match]])
    #?(:cljs [matthiasn.systems-toolbox.helpers :refer [request-animation-frame]])
    #?(:clj  [clojure.core.async :refer [<! >! chan put! sub pipe mult tap pub buffer sliding-buffer dropping-buffer go-loop timeout]]
       :cljs [cljs.core.async :refer [<! >! chan put! sub pipe mult tap pub buffer sliding-buffer dropping-buffer timeout]])
    #?(:clj  [clojure.tools.logging :as log])
    #?(:clj  [clojure.pprint :as pp]
       :cljs [cljs.pprint :as pp])
    #?(:cljs [cljs-uuid-utils.core :as uuid])))

#?(:clj  (defn now [] (System/currentTimeMillis))
   :cljs (defn now [] (.getTime (js/Date.))))

#?(:clj  (defn uuid [] (java.util.UUID/randomUUID))
   :cljs (defn uuid []  (uuid/make-random-uuid)))

(defn make-chan-w-buf
  "Create a channel with a buffer of the specified size and type."
  [config]
  (match config
         [:sliding n] (chan (sliding-buffer n))
         [:buffer n] (chan (buffer n))
         :else (prn "invalid: " config)))

(def component-defaults
  {:in-chan               [:buffer 1]
   :sliding-in-chan       [:sliding 1]
   :throttle-ms           1
   :out-chan              [:buffer 1]
   :sliding-out-chan      [:sliding 1]
   :firehose-chan         [:buffer 1]
   :snapshots-on-firehose true
   :msgs-on-firehose      true
   :reload-cmp            true})

(defn add-to-msg-seq
  "Function for adding the current component ID to the sequence that the message has traversed
  thus far. Component IDs can be added either when the message enters or leaves a component.
  There's a test to avoid conjoining the same ID twice in a row."
  [msg-meta cmp-id]
  (let [cmp-seq (vec (:cmp-seq msg-meta))]
    (if (= (last cmp-seq) cmp-id)
      msg-meta
      (assoc-in msg-meta [:cmp-seq] (conj cmp-seq cmp-id)))))

(defn msg-handler-loop
  "Constructs a map with a channel for the provided channel keyword, with the buffer
  configured according to cfg for the channel keyword. Then starts loop for taking messages
  off the returned channel and calling the provided handler-fn with the msg.
  Does not process return values from the processing step; instead, put-fn needs to be
  called to produce output."
  [{:keys [handler-map all-msgs-handler state-pub-handler put-fn cfg cmp-id firehose-chan snapshot-publish-fn]
    :as   cmp-map} cmp-state chan-key]
  (let [chan (make-chan-w-buf (chan-key cfg))]
    (go-loop []
      (let [msg (<! chan)
            msg-meta (-> (merge (meta msg) {})
                         (add-to-msg-seq cmp-id)
                         (assoc-in [cmp-id :in-ts] (now)))
            [msg-type msg-payload] msg
            handler-fn (msg-type handler-map)
            msg-map (merge cmp-map {:msg         (with-meta msg msg-meta)
                                    :msg-type    msg-type
                                    :msg-meta    msg-meta
                                    :msg-payload msg-payload
                                    :cmp-state   cmp-state})]
        (try
          (when (= chan-key :sliding-in-chan)
            (state-pub-handler msg-map)
            (when (and (:snapshots-on-firehose cfg) (not= "firehose" (namespace msg-type)))
              (put! firehose-chan [:firehose/cmp-recv-state {:cmp-id cmp-id :msg msg}]))
            (<! (timeout (:throttle-ms cfg))))
          (when (= chan-key :in-chan)
            (when (and (:msgs-on-firehose cfg) (not= "firehose" (namespace msg-type)))
              (put! firehose-chan [:firehose/cmp-recv {:cmp-id cmp-id :msg msg}]))
            (when (= msg-type :cmd/get-state) (put-fn [:state/snapshot {:cmp-id cmp-id :snapshot @cmp-state}]))
            (when (= msg-type :cmd/publish-state) (snapshot-publish-fn))
            (when handler-fn (handler-fn msg-map))
            (when all-msgs-handler (all-msgs-handler msg-map)))
          #?(:clj  (catch Exception e (do (log/error e "Exception in" cmp-id "when receiving message:")
                                          (pp/pprint msg)))
             :cljs (catch js/Object e (do (.log js/console e (str "Exception in " cmp-id " when receiving message:"))
                                          (pp/pprint msg)))))
        (recur)))
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
  [{:keys [cmp-id state-fn snapshot-xform-fn opts] :as cmp-conf}]
  (let [cfg (merge component-defaults opts)
        out-chan (make-chan-w-buf (:out-chan cfg))
        firehose-chan (make-chan-w-buf (:firehose-chan cfg))
        out-pub-chan (make-chan-w-buf (:out-chan cfg))
        sliding-out-chan (make-chan-w-buf (:sliding-out-chan cfg))
        put-fn (fn [msg]
                 (let [msg-meta (-> (merge (meta msg) {})
                                    (add-to-msg-seq cmp-id)
                                    (assoc-in [cmp-id :out-ts] (now)))
                       corr-id (uuid)
                       tag (or (:tag msg-meta) (uuid))
                       msg-w-meta (with-meta msg (merge msg-meta {:corr-id corr-id :tag tag}))]
                   (put! out-chan msg-w-meta)
                   (when (:msgs-on-firehose cfg)
                     (put! firehose-chan [:firehose/cmp-put {:cmp-id cmp-id :msg msg-w-meta}]))))
        out-mult (mult out-chan)
        firehose-mult (mult firehose-chan)
        state (if state-fn (state-fn put-fn) (atom {}))
        watch-state (if-let [watch (:watch cfg)] (watch state) state)
        changed (atom true)
        snapshot-publish-fn (fn []
                              (let [snapshot @watch-state
                                    snapshot-xform (if snapshot-xform-fn (snapshot-xform-fn snapshot) snapshot)
                                    snapshot-msg (with-meta [:app-state snapshot-xform] {:from cmp-id})]
                                (put! sliding-out-chan snapshot-msg)
                                (when (:snapshots-on-firehose cfg)
                                  (put! firehose-chan
                                        [:firehose/cmp-publish-state {:cmp-id cmp-id :snapshot snapshot-xform}]))))
        cmp-map (merge cmp-conf {:out-mult            out-mult
                                 :firehose-chan       firehose-chan
                                 :firehose-mult       firehose-mult
                                 :out-pub             (pub out-pub-chan first)
                                 :state-pub           (pub sliding-out-chan first)
                                 :cmp-state           state
                                 :watch-state         watch-state
                                 :put-fn              put-fn
                                 :snapshot-publish-fn snapshot-publish-fn
                                 :cfg                 cfg
                                 :state-snapshot-fn   (fn [] @watch-state)})]
    (tap out-mult out-pub-chan)
    #?(:clj  (try
               (add-watch watch-state :watcher (fn [_ _ _ new-state] (snapshot-publish-fn)))
               (catch Exception e (do (log/error e "Exception in" cmp-id "when attempting to watch atom:")
                                      (pp/pprint watch-state))))
       :cljs (letfn [(step []
                           (request-animation-frame step)
                           (when @changed
                             (snapshot-publish-fn)
                             (swap! changed not)))]
               (request-animation-frame step)
               (try (add-watch watch-state :watcher (fn [_ _ _ new-state] (reset! changed true)))
                    (catch js/Object e
                      (do (.log js/console e (str "Exception in " cmp-id " when attempting to watch atom:"))
                          (pp/pprint watch-state))))))
    (snapshot-publish-fn)
    (merge cmp-map
           (msg-handler-loop cmp-map state :in-chan)
           (msg-handler-loop cmp-map state :sliding-in-chan))))
