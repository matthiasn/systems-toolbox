(ns matthiasn.systems-toolbox.component
  (:require [matthiasn.systems-toolbox.spec :as s]
    #?(:clj [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])
    #?(:clj [io.aviso.exception :as ex])
            [matthiasn.systems-toolbox.component.helpers :as h]
            [matthiasn.systems-toolbox.component.msg-handling :as msg]
    #?(:clj [clojure.core.async :as a :refer [chan]]
       :cljs [cljs.core.async :as a :refer [chan]])))

(defn now [] (h/now))
(defn make-uuid [] (h/make-uuid))
(def send-msg msg/send-msg)
(def send-msgs msg/send-msgs)

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
   :reload-cmp            true
   :validate-in           false
   :validate-out          true
   :validate-state        true})

(defn make-snapshot-publish-fn
  "Creates a function for publishing changes to the component state atom as
   snapshot messages."
  [{:keys [watch-state snapshot-xform-fn cmp-id sliding-out-chan cfg
           firehose-chan]}]
  (fn []
    (when (:publish-snapshots cfg)
      (let [snapshot @watch-state
            snapshot-xform (if snapshot-xform-fn
                             (snapshot-xform-fn snapshot)
                             snapshot)
            snapshot-msg (with-meta [:app/state snapshot-xform] {:from cmp-id})
            state-firehose-chan (chan (a/sliding-buffer 1))]
        (a/pipe state-firehose-chan firehose-chan)
        (msg/put-msg sliding-out-chan snapshot-msg)
        (when (:snapshots-on-firehose cfg)
          (msg/put-msg state-firehose-chan
                       [:firehose/cmp-publish-state {:cmp-id      cmp-id
                                                     :firehose-id (h/make-uuid)
                                                     :snapshot    snapshot-xform
                                                     :ts          (now)}]))))))

(defn detect-changes
  "Detect changes to the component state atom and then publish a snapshot using
   the 'snapshot-publish-fn'."
  [{:keys [watch-state cmp-id snapshot-publish-fn]}]
  (try
    (add-watch watch-state cmp-id (fn [_ _ _ _new-state]
                                    (snapshot-publish-fn)))
    #?(:clj  (catch Exception e
               (l/error "Failed watching atom" cmp-id
                        (ex/format-exception e)
                        (h/pp-str watch-state)))
       :cljs (catch js/Object e (l/error e "Failed watching atom" cmp-id
                                         (h/pp-str watch-state))))))

(defn make-system-ready-fn
  "This function is called by the switchboard that wired this component when all
   other components are up and the channels between them connected. At this
   point, messages that were accumulated on the 'put-chan' buffer since startup
   are released. Also, the component state is published."
  [{:keys [put-chan out-chan snapshot-publish-fn]}]
  (fn []
    (a/pipe put-chan out-chan)
    (snapshot-publish-fn)))

(defn initial-cmp-map
  "Assembles initial component map with core.async channels.
    - :put-chan is used in component's put-fn, not connected at first
    - :out-chan is the outgoing channel
    - :firehose-chan is for where all messages go (for debugging)
    - :sliding-out-chan is for state snapshots
    "
  [cmp-map cfg]
  (merge cmp-map
         {:put-chan         (msg/make-chan-w-buf (:out-chan cfg))
          :out-chan         (msg/make-chan-w-buf (:out-chan cfg))
          :cfg              cfg
          :firehose-chan    (msg/make-chan-w-buf (:firehose-chan cfg))
          :sliding-out-chan (msg/make-chan-w-buf (:sliding-out-chan cfg))}))

(defn make-component
  "Creates a component with attached in-chan, out-chan, sliding-in-chan and
   sliding-out-chan.
   It takes the initial state atom, the handler function for messages on
   the in-chan, and the sliding-handler function, which handles messages on
   :sliding-in-chan.
   By default, in-chan and out-chan have standard buffers of size one, whereas
   sliding-in-chan and sliding-out-chan have sliding buffers of size one.
   The buffer sizes can be configured.
   The sliding-channels are meant for events where only ever the latest version
   is of interest, such as whenUI components rendering state snapshots from
   other components.
   Components send messages by using the put-fn, which is provided to the
   component when creating it's initial state, and then subsequently in every
   call to any of the handler functions. On every message send, a unique
   correlation ID is attached to every message.
   Also, messages are automatically assigned a tag, which is a unique ID that
   doesn't change when a message flows through the system. This tag can also be
   assigned manually by initially sending a message with the tag set on the
   metadata, as this tag will not be touched by the library whenever it exists
   already.
   The configuration of a component comes from merging the component defaults
   with the opts map that is passed on component creation the :opts key. The
   order of the merge operation allows overwriting the default settings.
   An observed-xform function can be provided, which transforms the observed
   state before resetting the respective observed state. This function takes a
   single argument, the observed state snapshot, and is expected to return a
   single map with the transformed snapshot."
  [{:keys [state-fn opts] :as cmp-map}]
  (try
    (let [cfg (merge component-defaults opts)
          out-pub-chan (msg/make-chan-w-buf (:out-chan cfg))
          cmp-map (initial-cmp-map cmp-map cfg)
          put-fn (msg/make-put-fn cmp-map)
          state-map
          (merge
            {:state    (atom {})
             :observed (atom {})}
            (when state-fn
              (let [new-state (state-fn put-fn)]
                (when-let [state-spec (:state-spec cmp-map)]
                  (when (:validate-state cfg)
                    (assert (s/valid-or-no-spec? state-spec @(:state new-state)))
                    (l/debug (:cmp-id cmp-map) "returned state validated")))
                new-state)))
          state (:state state-map)
          watch-state (if-let [watch (:watch opts)]         ; watchable atom
                        (watch state)
                        state)
          cmp-map (merge cmp-map {:watch-state watch-state})
          cmp-map (merge
                    cmp-map
                    {:snapshot-publish-fn (make-snapshot-publish-fn cmp-map)})
          cmp-map
          (merge cmp-map
                 {:out-mult          (a/mult (:out-chan cmp-map))
                  :firehose-mult     (a/mult (:firehose-chan cmp-map))
                  :out-pub           (a/pub out-pub-chan first)
                  :state-pub         (a/pub (:sliding-out-chan cmp-map) first)
                  :cmp-state         state
                  :observed          (:observed state-map)
                  :put-fn            put-fn
                  :system-ready-fn   (make-system-ready-fn cmp-map)
                  :shutdown-fn       (:shutdown-fn state-map)
                  :state-snapshot-fn (fn [] @watch-state)
                  :state-reset-fn    (fn [new-state]
                                       (reset! watch-state new-state))})]
      (a/tap (:out-mult cmp-map) out-pub-chan)              ; connect out-pub-chan to out-mult
      (detect-changes cmp-map)                              ; publish snapshots when changes are detected
      (merge cmp-map
             (msg/msg-handler-loop cmp-map :in-chan)
             (msg/msg-handler-loop cmp-map :sliding-in-chan)))
    #?(:clj  (catch Exception e (l/error "Failed to init" (:cmp-id cmp-map)
                                         (ex/format-exception e))
                                (System/exit 1))
       :cljs (catch js/Object e (l/error "Failed to init" (:cmp-id cmp-map) e)))))
