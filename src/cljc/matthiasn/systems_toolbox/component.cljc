(ns matthiasn.systems-toolbox.component
  #?(:cljs (:require-macros [cljs.core.async.macros :as cam :refer [go go-loop]]
                            [cljs.core :refer [exists?]]))
  (:require  [matthiasn.systems-toolbox.spec :as s]
             [matthiasn.systems-toolbox.log :as l]
    #?(:clj  [clojure.core.match :refer [match]]
       :cljs [cljs.core.match :refer-macros [match]])
    #?(:clj  [clojure.core.async :as a :refer [chan go go-loop]]
       :cljs [cljs.core.async :as a :refer [chan]])
    #?(:clj  [clojure.tools.logging :as log])
    #?(:clj  [clojure.pprint :as pp]
       :cljs [cljs.pprint :as pp])
    #?(:cljs [cljs-uuid-utils.core :as uuid])))

(defn now
  "Get milliseconds since epoch."
  []
  #?(:clj  (System/currentTimeMillis)
     :cljs (.getTime (js/Date.))))

(defn pp-str [data] (with-out-str (pp/pprint data)))

(defn put-msg
  "On the JVM, always use the blocking operation for putting messages on a channel, as otherwise the system easily
  blows up when there are more than 1024 pending put operations. On the ClojureScript side, there isno equivalent
  of the blocking put, so the asynchronous operation will have to do. But then, more than 1024 pending operations
  in the browser wouldn't happen often, if ever."
  [channel msg]
  #?(:clj  (a/>!! channel msg)
     :cljs (a/put! channel msg)))

(defn make-uuid
  "Get a random UUID."
  []
  #?(:clj  (str (java.util.UUID/randomUUID))
     :cljs (str (uuid/make-random-uuid))))

#?(:cljs (def request-animation-frame
           (or (when (exists? js/window)
                 (or (.-requestAnimationFrame js/window)
                     (.-webkitRequestAnimationFrame js/window)
                     (.-mozRequestAnimationFrame js/window)
                     (.-msRequestAnimationFrame js/window)))
               (fn [callback] (js/setTimeout callback 17)))))

(defn make-chan-w-buf
  "Create a channel with a buffer of the specified size and type."
  [config]
  (match config
         [:sliding n] (chan (a/sliding-buffer n))
         [:buffer  n] (chan (a/buffer n))
         :else (prn "invalid: " config)))

(def component-defaults
  {:in-chan               [:buffer 1]
   :sliding-in-chan       [:sliding 1]
   :throttle-ms           1
   :out-chan              [:buffer 1]
   :sliding-out-chan      [:sliding 1]
   :firehose-chan         [:buffer 1]
   :publish-snapshots     true
   :snapshots-on-firehose false
   :msgs-on-firehose      false
   :reload-cmp            true})

(defn add-to-msg-seq
  "Function for adding the current component ID to the sequence that the message has traversed
  thus far. The specified component IDs is either added when the cmp-seq is empty in the case
  of an initial send or when the message is received by a component. This avoids recording
  component IDs multiple times."
  [msg-meta cmp-id in-out]
  (let [cmp-seq (vec (:cmp-seq msg-meta))]
    (if (or (empty? cmp-seq) (= in-out :in))
      (assoc-in msg-meta [:cmp-seq] (conj cmp-seq cmp-id))
      msg-meta)))

(defn default-state-pub-handler
  "Default handler function, can be replaced by a more application-specific handler function, for example
  for resetting local component state when user is not logged in."
  [{:keys [cmp-state msg-payload observed-xform]}]
  (let [new-state (if observed-xform
                    (observed-xform msg-payload)
                    msg-payload)]
    (when (not= @(:observed cmp-state) new-state)
      (reset! (:observed cmp-state) new-state))))

(defn msg-handler-loop
  "Constructs a map with a channel for the provided channel keyword, with the buffer
  configured according to cfg for the channel keyword. Then starts loop for taking messages
  off the returned channel and calling the provided handler-fn with the msg.
  Uses return value from handler function to change state and emit messages if there respective
  keys :new-state and :emit-msg exist. Thus, the handler function can be free from side effects.
  For backwards compatibility, it is also possible interact with the put-fn and the cmp-state
  atom directly, in which case the handler function itself would produce side effects.
  This, however, makes such handler functions somewhat more difficult to test."
  [cmp-map chan-key]
  (let [{:keys [handler-map all-msgs-handler state-pub-handler cfg cmp-id firehose-chan snapshot-publish-fn
                unhandled-handler state-reset-fn state-snapshot-fn put-fn]
         :or {handler-map {}}} cmp-map
        in-chan (make-chan-w-buf (chan-key cfg))]
    (go-loop []
      (let [msg (a/<! in-chan)
            msg-meta (-> (merge (meta msg) {})
                         (add-to-msg-seq cmp-id :in)
                         (assoc-in [cmp-id :in-ts] (now)))
            [msg-type msg-payload] msg
            handler-fn (msg-type handler-map)
            msg-map (merge cmp-map {:msg           (with-meta msg msg-meta)
                                    :msg-type      msg-type
                                    :msg-meta      msg-meta
                                    :msg-payload   msg-payload
                                    :onto-in-chan  #(a/onto-chan in-chan % false)
                                    :current-state (state-snapshot-fn)})
            state-change-emit-handler (fn [{:keys [new-state emit-msg emit-msgs send-to-self]}]
                                        (when new-state
                                          (when-let [state-spec (:state-spec cmp-map)]
                                            (s/valid-or-no-spec? state-spec new-state))
                                          (state-reset-fn new-state))
                                        (when send-to-self
                                          (if (vector? (first send-to-self))
                                            (a/onto-chan in-chan send-to-self false)
                                            (a/onto-chan in-chan [send-to-self] false)))
                                        (let [emit-msg-fn (fn [msg-to-emit]
                                                            (let [new-meta (meta msg-to-emit)]
                                                              (put-fn (with-meta msg-to-emit
                                                                                 (or new-meta msg-meta)))))]
                                          (when emit-msg
                                            (if (vector? (first emit-msg))
                                              (doseq [msg-to-emit emit-msg]
                                                (emit-msg-fn msg-to-emit))
                                              (emit-msg-fn emit-msg)))
                                          (when emit-msgs
                                            (l/warn "DEPRECATED use of emit-msgs, use emit-msg with a message vector instead")
                                            (doseq [msg-to-emit emit-msgs]
                                              (emit-msg-fn msg-to-emit)))))]
        (try
          (s/valid-or-no-spec? msg-type msg-payload)
          (when (= chan-key :sliding-in-chan)
            (state-change-emit-handler ((or state-pub-handler default-state-pub-handler) msg-map))
            (when (and (:snapshots-on-firehose cfg) (not= "firehose" (namespace msg-type)))
              (put-msg firehose-chan [:firehose/cmp-recv-state {:cmp-id cmp-id :msg msg}]))
            (a/<! (a/timeout (:throttle-ms cfg))))
          (when (= chan-key :in-chan)
            (when (and (:msgs-on-firehose cfg) (not= "firehose" (namespace msg-type)))
              (put-msg firehose-chan [:firehose/cmp-recv {:cmp-id   cmp-id
                                                          :msg      msg
                                                          :msg-meta msg-meta
                                                          :ts       (now)}]))
            (when (= msg-type :cmd/publish-state) (snapshot-publish-fn))
            (when handler-fn (state-change-emit-handler (handler-fn msg-map)))
            (when unhandled-handler
              (when-not (contains? handler-map msg-type) (state-change-emit-handler (unhandled-handler msg-map))))
            (when all-msgs-handler (state-change-emit-handler (all-msgs-handler msg-map))))
          #?(:clj  (catch Exception e (log/error e "Exception in" cmp-id "when receiving message:" (pp-str msg)))
             :cljs (catch js/Object e (.log js/console e (str "Exception in " cmp-id " when receiving message:"
                                                              (pp-str msg))))))
        (recur)))
    {chan-key in-chan}))

(defn make-put-fn
  "The put-fn is used inside each component for emitting messages to the outside world, from
   the component's point of view. All the component needs to know is the type of the message.
   Messages are vectors of two elements, where the first one is the type as a namespaced keyword
   and the second one is the message payload, like this: [:some/msg-type {:some \"data\"}]
   Message payloads are typically maps or vectors, but they can also be strings, primitive types
   nil. As long as they are local, they can even be any type, e.g. a channel, but once we want
   messages to traverse some message transport (WebSockets, some message queue), the types
   should be limited to what EDN or Transit can encode.
   Note that on component startup, this channel is not wired anywhere until the 'system-ready-fn'
   (below) is called, which pipes this channel into the actual out-chan. Thus, components should
   not try call more messages than fit in the buffer before the entire system is up."
  [{:keys [cmp-id put-chan cfg firehose-chan]}]
  (fn [msg]
    {:pre [(s/valid-or-no-spec? (first msg) (second msg))]}
    (s/valid-or-no-spec? :systems-toolbox/msg msg)
    (let [msg-meta (-> (merge (meta msg) {})
                       (add-to-msg-seq cmp-id :out)
                       (assoc-in [cmp-id :out-ts] (now)))
          corr-id (make-uuid)
          tag (or (:tag msg-meta) (make-uuid))
          completed-meta (merge msg-meta {:corr-id corr-id :tag tag})
          msg-w-meta (with-meta msg completed-meta)
          msg-type (first msg)
          msg-payload (second msg)
          msg-from-firehose? (= "firehose" (namespace msg-type))]
      ;(s/valid-or-no-spec? msg-type msg-payload)
      (put-msg put-chan msg-w-meta)

      ;; Not all components should emit firehose messages. For example, messages that process
      ;; firehose messages should not do so again in order to avoid infinite messaging loops.
      ;; This behavior can be configured when the component is fired up.
      (when (:msgs-on-firehose cfg)
        ;; Some components may emit firehose messages directly. One such example is the
        ;; WebSockets component which can be used for relaying firehose messages, either
        ;; from client to server or from server to client. In those cases, the emitted
        ;; message should go on the firehose channel on the receiving end as such, not
        ;; wrapped as other messages would (see the second case in the if-clause).
        (if msg-from-firehose?
          (put-msg firehose-chan msg-w-meta)
          (put-msg firehose-chan [:firehose/cmp-put {:cmp-id   cmp-id
                                                     :msg      msg-w-meta
                                                     :msg-meta completed-meta
                                                     :ts       (now)}]))))))

(defn make-snapshot-publish-fn
  "Creates a function for publishing changes to the component state atom as snapshot messages,"
  [{:keys [watch-state snapshot-xform-fn cmp-id sliding-out-chan cfg firehose-chan]}]
  (fn []
    (when (:publish-snapshots cfg)
      (let [snapshot @watch-state
            snapshot-xform (if snapshot-xform-fn (snapshot-xform-fn snapshot) snapshot)
            snapshot-msg (with-meta [:app/state snapshot-xform] {:from cmp-id})
            state-firehose-chan (chan (a/sliding-buffer 1))]
        (a/pipe state-firehose-chan firehose-chan)
        (put-msg sliding-out-chan snapshot-msg)
        (when (:snapshots-on-firehose cfg)
          (put-msg state-firehose-chan [:firehose/cmp-publish-state {:cmp-id   cmp-id
                                                                     :snapshot snapshot-xform
                                                                     :ts       (now)}]))))))

(defn detect-changes
  "Detect changes to the component state atom and then publish a snapshot using the
  'snapshot-publish-fn'.
  The Clojure version simply calls the snapshot-publish-fn whenever there is a change
  to the component state atom.
  The ClojureScript version holds the publication of a new component state snapshot by
  scheduling it to happen on the the next animation-frame event. Until then, no further
  snapshot publications are scheduled. This effectively ignores all state updates until
  that event fires, while then publishing the latest snapshot. This mechanism avoids
  burdening the JS engine with messages that are not relevant for rendering anyway."
  [{:keys [watch-state cmp-id snapshot-publish-fn]}]
  #?(:clj  (try (add-watch watch-state :watcher (fn [_ _ _ _new-state] (snapshot-publish-fn)))
                (catch Exception e (log/error e "Exception in" cmp-id "when watching atom:"
                                              (pp-str watch-state))))
     :cljs (let [publish-scheduled? (atom false)
                 publish-fn (fn []
                              (snapshot-publish-fn)
                              (reset! publish-scheduled? false))
                 publish-schedule-fn (fn []
                                       (when-not @publish-scheduled?
                                         (reset! publish-scheduled? true)
                                         (request-animation-frame publish-fn)))]
             (try (add-watch watch-state :watcher (fn [_ _ _ _new-state] (publish-schedule-fn)))
                  (catch js/Object e
                    (.log js/console e (str "Exception in " cmp-id " when watching atom:"
                                            (pp-str watch-state))))))))

(defn make-system-ready-fn
  "This function is called by the switchboard that wired this component when all other
  components are up and the channels between them connected. At this point, messages that
  were accumulated on the 'put-chan' buffer since startup are released. Also, the
  component state is published."
  [{:keys [put-chan out-chan snapshot-publish-fn]}]
  (fn []
    (a/pipe put-chan out-chan)
    (snapshot-publish-fn)))

(defn initial-cmp-map
  "Assembles initial component map with actual channels."
  [cmp-map cfg]
  (merge cmp-map
         {:put-chan         (make-chan-w-buf (:out-chan cfg))            ; used in put-fn, not connected at first
          :out-chan         (make-chan-w-buf (:out-chan cfg))            ; outgoing chan, used in mult and pub
          :cfg              cfg
          :firehose-chan    (make-chan-w-buf (:firehose-chan cfg))       ; channel for publishing all messages
          :sliding-out-chan (make-chan-w-buf (:sliding-out-chan cfg))})) ; chan for publishing snapshots

(defn make-component
  "Creates a component with attached in-chan, out-chan, sliding-in-chan and sliding-out-chan.
  It takes the initial state atom, the handler function for messages on in-chan, and the
  sliding-handler function, which handles messages on sliding-in-chan.
  By default, in-chan and out-chan have standard buffers of size one, whereas sliding-in-chan
  and sliding-out-chan have sliding buffers of size one. The buffer sizes can be configured.
  The sliding-channels are meant for events where only ever the latest version is of interest,
  such as mouse moves or published state snapshots in the case of UI components rendering
  state snapshots from other components.
  Components send messages by using the put-fn, which is provided to the component when
  creating it's initial state, and then subsequently in every call to any of the handler
  functions. On every message send, a unique correlation ID is attached to every message.
  Also, messages are automatically assigned a tag, which is a unique ID that doesn't change
  when a message flows through the system. This tag can also be assigned manually by
  initially sending a message with the tag set on the metadata, as this tag will not be
  touched by the library whenever it exists already.
  The configuration of a component comes from merging the component defaults with the opts
  map that is passed on component creation the :opts key. The order of the merge operation
  allows overwriting the default settings.
  An observed-xform function can be provided, which transforms the observed state before
  resetting the respective observed state. This function takes a single argument, the observed
  state snapshot, and is expected to return a single map with the transformed snapshot."
  [{:keys [state-fn opts] :as cmp-map}]
  (let [cfg (merge component-defaults opts)
        out-pub-chan (make-chan-w-buf (:out-chan cfg))
        cmp-map (initial-cmp-map cmp-map cfg)
        put-fn (make-put-fn cmp-map)
        state-map (if state-fn (state-fn put-fn) {:state (atom {})}) ; create state, either from state-fn or empty map
        state (:state state-map)
        watch-state (if-let [watch (:watch opts)] (watch state) state) ; watchable atom
        cmp-map (merge cmp-map {:watch-state watch-state})
        cmp-map (merge cmp-map {:snapshot-publish-fn (make-snapshot-publish-fn cmp-map)})
        cmp-map (merge cmp-map {:out-mult          (a/mult (:out-chan cmp-map))
                                :firehose-mult     (a/mult (:firehose-chan cmp-map))
                                :out-pub           (a/pub out-pub-chan first)
                                :state-pub         (a/pub (:sliding-out-chan cmp-map) first)
                                :cmp-state         state
                                :put-fn            put-fn
                                :system-ready-fn   (make-system-ready-fn cmp-map)
                                :shutdown-fn       (:shutdown-fn state-map)
                                :state-snapshot-fn (fn [] @watch-state)
                                :state-reset-fn    (fn [new-state] (reset! watch-state new-state))})]
    (a/tap (:out-mult cmp-map) out-pub-chan)  ; connect out-pub-chan to out-mult
    (detect-changes cmp-map)                ; publish snapshots when changes are detected
    (merge cmp-map
           (msg-handler-loop cmp-map :in-chan)
           (msg-handler-loop cmp-map :sliding-in-chan))))

(defn send-msg
  "Sends message to the specified component. By default, calls to this function will block when no buffer space
  is available. Asynchronous handling is also possible (JVM only), however the implications should be understood,
  such as that core.async will throw an exception when there are more than 1024 pending operations. Under most
  circumstances, blocking seems like the safer bet. Note that, unless specified otherwise, the buffer for a
  component's in-chan is of size one, see 'component-defaults'."
  ([cmp msg] (send-msg cmp msg true))
  ([cmp msg blocking?]
   (let [in-chan (:in-chan cmp)]
     #?(:clj  (if blocking?
                (a/>!! in-chan msg)
                (a/put! in-chan msg))
        :cljs (a/put! in-chan msg)))))

(defn send-msgs
  "Sends multiple messages to a component. Takes the component itself plus a sequence with messages to send
  to the component. Does not close the :in-chan of the component."
  [cmp msgs]
  (let [in-chan (:in-chan cmp)]
    (a/onto-chan in-chan msgs false)))
