(ns matthiasn.systems-toolbox.component.msg-handling
  #?(:cljs (:require-macros [cljs.core.async.macros :as cam :refer [go-loop]]
             [cljs.core :refer [exists?]]))
  (:require [matthiasn.systems-toolbox.spec :as spec]
    #?(:clj [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])
            [matthiasn.systems-toolbox.component.helpers :as h]
    #?(:clj [io.aviso.exception :as ex])
    #?(:clj [clojure.core.match :refer [match]]
       :cljs [cljs.core.match :refer-macros [match]])
    #?(:clj [clojure.core.async :as a :refer [chan go-loop]]
       :cljs [cljs.core.async :as a :refer [chan]])
            [clojure.set :as s]))

(defn put-msg
  "On the JVM, always uses the blocking operation for putting messages on a
   channel, as otherwise the system easily blows up when there are more than 1024
   pending put operations. On the ClojureScript side, there is no equivalent of
   the blocking put, so the asynchronous operation will have to do. But then,
   more than 1024 pending operations in the browser wouldn't happen often,
   if ever."
  [channel msg]
  #?(:clj  (a/>!! channel msg)
     :cljs (a/put! channel msg)))

(defn make-chan-w-buf
  "Create a channel with a buffer of the specified size and type."
  [config]
  (match config
         [:sliding n] (chan (a/sliding-buffer n))
         [:buffer n] (chan (a/buffer n))
         :else (prn "invalid: " config)))

(defn add-to-msg-seq
  "Function for adding the current component ID to the sequence that the message
   has traversed thus far. The specified component IDs is either added when the
   cmp-seq is empty in the case of an initial send or when the message is
   received by a component. This avoids recording component IDs multiple times."
  [msg-meta cmp-id in-out]
  (let [cmp-seq (vec (:cmp-seq msg-meta))]
    (if (or (empty? cmp-seq) (= in-out :in))
      (assoc-in msg-meta [:cmp-seq] (conj cmp-seq cmp-id))
      msg-meta)))

(defn default-state-pub-handler
  "Default handler function, can be replaced by a more application-specific
   handler function, for example for resetting local component state when user
   is not logged in."
  [{:keys [observed msg-payload observed-xform]}]
  (let [new-state (if observed-xform (observed-xform msg-payload) msg-payload)]
    (when (not= @observed new-state)
      (reset! observed new-state))
    {}))

(defn mk-handler-return-fn
  "Returns function for handling the return value of a handler function.
  This returned map can contain :new-state, :emit-msg and :send-to-self keys."
  [{:keys [state-reset-fn cfg put-fn cmp-id] :as cmp-map} in-chan msg-type msg-meta]
  (fn [{:keys [new-state emit-msg emit-msgs send-to-self] :as handler-res}]
    (l/debug cmp-id "handler returned")
    (let [emit-msg-fn (fn [msg]
                        (when (seq msg)
                          (put-fn (with-meta msg (or (meta msg) msg-meta)))))]
      (when new-state (when-let [state-spec (:state-spec cmp-map)]
                        (when (:validate-state cfg)
                          (assert (spec/valid-or-no-spec? state-spec new-state))
                          (l/debug cmp-id "returned state validated")))
                      (state-reset-fn new-state))
      (when send-to-self (if (vector? (first send-to-self))
                           (a/onto-chan in-chan send-to-self false)
                           (a/onto-chan in-chan [send-to-self] false)))
      (when emit-msg (if (vector? (first emit-msg))
                       (doseq [msg-to-emit emit-msg] (emit-msg-fn msg-to-emit))
                       (emit-msg-fn emit-msg)))
      (let [res-keys (set (keys handler-res))
            known-keys #{:new-state :emit-msg :emit-msgs :send-to-self}]
        (when-not (s/subset? res-keys known-keys)
          (l/warn "Unknown keys in handler result. THIS IS PROBABLY NOT WHAT YOU WANT."
                  (s/difference res-keys known-keys)
                  cmp-id
                  msg-type)))
      (when emit-msgs
        (l/warn "DEPRECATED: emit-msgs, use emit-msg with a msg vector instead")
        (doseq [msg-to-emit emit-msgs]
          (emit-msg-fn msg-to-emit))))))

(defn msg-handler-loop
  "Constructs a map with a channel for the provided channel keyword, with the
   buffer configured according to cfg for the channel keyword. Then starts loop
   for taking messages off the returned channel and calling the provided
   handler-fn with the msg.
   Uses return value from handler function to change state and emit messages if
   the respective keys :new-state and :emit-msg exist. Thus, the handler
   function can be free from side effects.
   For backwards compatibility, it is also possible interact with the put-fn and
   the cmp-state atom directly, in which case the handler function itself would
   produce side effects.
   This, however, makes such handler functions somewhat more difficult to test."
  [cmp-map chan-key]
  (let [{:keys [handler-map all-msgs-handler state-pub-handler cfg cmp-id
                firehose-chan snapshot-publish-fn unhandled-handler
                state-snapshot-fn]
         :or   {handler-map {}}} cmp-map
        in-chan (make-chan-w-buf (chan-key cfg))
        onto-in-chan #(a/onto-chan in-chan % false)]
    (go-loop []
      (let [msg (a/<! in-chan)]
        (l/debug cmp-id "msg received" msg)
        (try
          (let [msg-meta (-> (merge (meta msg) {})
                             (add-to-msg-seq cmp-id :in)
                             (assoc-in [cmp-id :in-ts] (h/now)))
                [msg-type msg-payload] msg
                handler-fn (msg-type handler-map)
                msg-map-fn
                (fn []
                  (merge cmp-map {:msg           (with-meta msg msg-meta)
                                  :msg-type      msg-type
                                  :msg-meta      msg-meta
                                  :msg-payload   msg-payload
                                  :onto-in-chan  onto-in-chan
                                  :current-state (state-snapshot-fn)}))
                handler-return-fn (mk-handler-return-fn cmp-map in-chan msg-type msg-meta)
                observed-state-handler (or state-pub-handler
                                           default-state-pub-handler)]
            (when (:validate-in cfg)
              (assert (spec/valid-or-no-spec? msg-type msg-payload)))
            (l/debug cmp-id "msg validated")
            (when (= chan-key :sliding-in-chan)
              (handler-return-fn (observed-state-handler (msg-map-fn)))
              (l/debug cmp-id "observed-state-handler done")
              (when (and (:snapshots-on-firehose cfg)
                         (not= "firehose" (namespace msg-type)))
                (put-msg firehose-chan
                         [:firehose/cmp-recv-state {:cmp-id cmp-id :msg msg}]))
              (l/debug cmp-id "state snapshot published on firehose")
              (a/<! (a/timeout (:throttle-ms cfg))))
            (when (= chan-key :in-chan)
              (when (= msg-type :cmd/publish-state) (snapshot-publish-fn))
              (when handler-fn
                (handler-return-fn (handler-fn (msg-map-fn)))
                (l/debug cmp-id "handler function done"))
              (when unhandled-handler
                (when-not (contains? handler-map msg-type)
                  (handler-return-fn (unhandled-handler (msg-map-fn)))
                  (l/debug cmp-id "unhandled-handler function done")))
              (when all-msgs-handler
                (handler-return-fn (all-msgs-handler (msg-map-fn)))
                (l/debug cmp-id "all-msgs-handler function done"))
              (when (and (:msgs-on-firehose cfg)
                         (not= "firehose" (namespace msg-type)))
                (put-msg firehose-chan [:firehose/cmp-recv
                                        {:cmp-id      cmp-id
                                         :firehose-id (h/make-uuid)
                                         :msg         msg
                                         :msg-meta    msg-meta
                                         :ts          (h/now)}]))
              (l/debug cmp-id "received message published on firehose")))
          #?(:clj  (catch Exception e
                     (l/error "Exception in" cmp-id "when receiving message:"
                              (ex/format-exception e) (h/pp-str msg)))
             :cljs (catch js/Object e
                     (l/error e
                       (str "Exception in " cmp-id " when receiving message:"
                            (h/pp-str msg)))))
          #?(:clj  (catch AssertionError e
                     (l/error "AssertionError in" cmp-id "when receiving message:"
                              (ex/format-exception e) (h/pp-str msg)))))
        (recur)))
    {chan-key in-chan}))

(defn make-put-fn
  "The put-fn is used inside each component for emitting messages to the outside
   world, from the component's point of view. All the component needs to know is
   the type of the message.
   Messages are vectors of two elements, where the first one is the type as a
   namespaced keyword and the second one is the message payload, like this:
     [:some/msg-type {:some \"data\"}]
   Message payloads are typically maps or vectors, but they can also be strings,
   primitive types, or nil. As long as they are local, they can even be any
   type, e.g. a channel, but once we want messages to traverse some message
   transport (WebSockets, some message queue), the types need to be limited to
   what EDN or Transit can serialize.
   Note that on component startup, this channel is not wired anywhere until the
   'system-ready-fn' (below) is called, which pipes this channel into the actual
   out-chan. Thus, components should not try call more messages than fit in the
   buffer before the entire system is up."
  [{:keys [cmp-id put-chan cfg firehose-chan]}]
  (let [put-one-msg
        (fn [msg]
          (try
            (l/debug cmp-id "put-fn called")
            (when (:validate-out cfg)
              (assert (spec/valid-or-no-spec? (first msg) (second msg)))
              (l/debug cmp-id "put-fn msg validated"))
            (let [msg-meta (-> (merge (meta msg) {})
                               (add-to-msg-seq cmp-id :out)
                               (assoc-in [cmp-id :out-ts] (h/now)))
                  corr-id (h/make-uuid)
                  tag (or (:tag msg-meta) (h/make-uuid))
                  completed-meta (merge msg-meta {:corr-id corr-id :tag tag})
                  msg-w-meta (with-meta msg completed-meta)
                  msg-type (first msg)
                  msg-from-firehose? (= "firehose" (namespace msg-type))]
              (put-msg put-chan msg-w-meta)
              (l/debug cmp-id "put-fn: msg sent")
              ;; Not all components should emit firehose messages. For example, messages
              ;; that process firehose messages should not do so again in order to avoid
              ;; infinite messaging loops.
              ;; This behavior can be configured when the component is fired up.
              (when (:msgs-on-firehose cfg)
                ;; Some components may emit firehose messages directly. One such example
                ;; is the WebSockets component which can be used for relaying firehose
                ;; messages, either from client to server or from server to client.
                ;; In those cases, the emitted message should go on the firehose channel
                ;; on the receiving end as such, not wrapped as other messages would
                ;; (see the second case in the if-clause).
                (if msg-from-firehose?
                  (put-msg firehose-chan msg-w-meta)
                  (put-msg firehose-chan
                           [:firehose/cmp-put {:cmp-id      cmp-id
                                               :firehose-id (h/make-uuid)
                                               :msg         msg-w-meta
                                               :msg-meta    completed-meta
                                               :ts          (h/now)}]))
                (l/debug cmp-id "put-fn: msg put on firehose")))
            #?(:clj  (catch Exception e
                       (l/error "Exception in" cmp-id "when sending message:"
                                (ex/format-exception e) (h/pp-str msg)))
               :cljs (catch js/Object e
                       (l/error e
                                (str "Exception in " cmp-id " when sending message:"
                                     (h/pp-str msg)))))
            #?(:clj (catch AssertionError e
                      (l/error "AssertionError in" cmp-id "when sending message:"
                               (ex/format-exception e) (h/pp-str msg))))))]
    (fn [m]
      (if (vector? (first m))
        (doseq [msg m] (put-one-msg msg))
        (put-one-msg m)))))

(defn send-msg
  "Sends message to the specified component. By default, calls to this function
   will block when no buffer space is available. Asynchronous handling is also
   possible (JVM only), however the implications should be understood, such as
   that core.async will throw an exception when there are more than 1024 pending
   operations. Under most circumstances, blocking seems like the safer bet.
   Note that, unless specified otherwise, the buffer for a component's in-chan
   is of size one, see 'component-defaults'."
  ([cmp msg] (send-msg cmp msg true))
  ([cmp msg blocking?]
   (let [in-chan (:in-chan cmp)]
     #?(:clj  (if blocking?
                (a/>!! in-chan msg)
                (a/put! in-chan msg))
        :cljs (a/put! in-chan msg)))))

(defn send-msgs
  "Sends multiple messages to a component. Takes the component itself plus a
   sequence with messages to send to the component.
   Does not close the :in-chan of the component."
  [cmp msgs]
  (let [in-chan (:in-chan cmp)]
    (a/onto-chan in-chan msgs false)))
