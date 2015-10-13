(ns matthiasn.systems-toolbox.switchboard
  #?(:clj (:gen-class))
  (:require
    #?(:clj  [clojure.core.match :refer [match]]
       :cljs [cljs.core.match :refer-macros [match]])
    #?(:clj  [clojure.core.async :refer [put! sub tap untap-all unsub-all]]
       :cljs [cljs.core.async :refer [put! sub tap untap-all unsub-all]])
    #?(:clj  [clojure.pprint :as pp]
       :cljs [cljs.pprint :as pp])
    [matthiasn.systems-toolbox.component :as comp]))

(defn wire-comp
  "Wire existing and already instantiated component. Also capable of reloading component, e.g. when using Figwheel on
  the client side. When a previous component with the same name exists, this function first of all unwires that previous
  component by unsubscribing and untapping all connected channels. Then, the state of that previous component is
  used in the new component in order to provide a smooth developer experience. When the either is no previous
  component with the same name or the component ought to be reloaded, the previous one is replaced by the new one in the
  switchboard state. Finally, the new component is tapped into the switchboard's firehose and the component is also
  asked to publish its state once (also useful for Figwheel)."
  [{:keys [cmp-state put-fn msg-payload cmp-id]}]
  (let [switchbrd-snapshot @cmp-state]
    (doseq [cmp (flatten [msg-payload])]
      (let [cmp-id-to-wire (:cmp-id cmp)
            firehose-chan (:firehose-chan (cmp-id (:components @cmp-state)))
            in-chan (:in-chan cmp)
            reload? (:reload-cmp (:cfg cmp))
            prev-cmp (get-in switchbrd-snapshot [:components cmp-id-to-wire])]
        (when prev-cmp (untap-all (:firehose-mult prev-cmp))
                       (unsub-all (:out-pub prev-cmp))
                       (unsub-all (:state-pub prev-cmp))
                       (let [prev-state (:watch-state prev-cmp)]
                         (reset! (:watch-state cmp) @prev-state)))
        (when (or (not prev-cmp) reload?) (swap! cmp-state assoc-in [:components cmp-id-to-wire] cmp))
        (tap (:firehose-mult cmp) firehose-chan)
        (swap! cmp-state update-in [:fh-taps] conj {:from cmp-id-to-wire :to cmp-id :type :fh-tap})
        (put! in-chan [:cmd/publish-state])))))


(defn subscribe
  "Subscribe component to a specified publisher."
  [app put-fn [from-cmp from-pub] msg-type [to-cmp to-chan]]
  (let [pub-comp (from-cmp (:components @app))
        sub-comp (to-cmp (:components @app))]
    (sub (from-pub pub-comp) msg-type (to-chan sub-comp))
    (swap! app update-in [:subs] conj {:from from-cmp :to to-cmp :msg-type msg-type :type :sub})))

(defn subscribe-comp-state
  "Subscribe component to a specified publisher."
  [app put-fn [from to]]
  (doseq [t (flatten [to])]
    (subscribe app put-fn [from :state-pub] :app-state [t :sliding-in-chan])))

(defn tap-components
  "Tap into a mult."
  [app put-fn [from-cmps to]]
  (doseq [from (flatten [from-cmps])]
    (let [mult-comp (from (:components @app))
          tap-comp (to (:components @app))
          err-put #(put-fn [:log/switchboard-tap (str "Could not create tap: " from " -> " to " - " %)])]
      (try (do
             (tap (:out-mult mult-comp) (:in-chan tap-comp))
             (swap! app update-in [:taps] conj {:from from :to to :type :tap}))
           #?(:clj (catch Exception e (err-put (.getMessage e)))
              :cljs (catch js/Object e (err-put e)))))))

(defn tap-switchboard-firehose
  "Tap the switchboard firehose into a component observing it."
  [app put-fn to switchboard-id]
  (let [sw-firehose-mult (:firehose-mult (switchboard-id (:components @app)))
        to-comp (to (:components @app))
        err-put #(put-fn [:log/switchboard-tap (str "Could not create tap: " switchboard-id " -> " to " - " %)])]
    (try (do
           (tap sw-firehose-mult (:in-chan to-comp))
           (swap! app update-in [:fh-taps] conj {:from switchboard-id :to to :type :fh-tap}))
         #?(:clj (catch Exception e (err-put (.getMessage e)))
            :cljs (catch js/Object e (err-put e))))))

(defn- self-register
  "Registers switchboard itself as another component that can be wired. Useful
  for communication with the outside world / within hierarchies where a subsystem
  has its own switchboard."
  [{:keys [cmp-state msg-payload cmp-id]}]
  (swap! cmp-state assoc-in [:components cmp-id] msg-payload)
  (swap! cmp-state assoc-in [:switchboard-id] cmp-id))

(defn mk-state
  "Create initial state atom for switchboard component."
  [put-fn]
  (atom {:components {} :subs #{} :taps #{} :fh-taps #{}}))

(defn route-handler
  "Creates subscriptions between one component's out-pub and another component's in-chan.
  Requires a map with at least the :from and :to keys.
  Also, routing can be limited to message types specified under the :only keyword. Here, either
  a single message type or a vector with multiple message types can be used."
  [{:keys [cmp-state put-fn msg-payload]}]
  (let [{:keys [from to only]} msg-payload
        handled-messages (keys (:handler-map (to (:components @cmp-state))))
        msg-types (if only (flatten [only]) (vec handled-messages))]
    (doseq [msg-type msg-types]
      (subscribe cmp-state put-fn [from :out-pub] msg-type [to :in-chan]))))

(defn route-all-handler
  [{:keys [cmp-state put-fn msg-payload]}]
  (let [{:keys [from to]} msg-payload]
    (tap-components cmp-state put-fn [from to])))

(defn attach-to-firehose
  "Attaches a component to firehose channel. For example for observational components."
  [{:keys [cmp-state put-fn msg-payload cmp-id]}]
  (tap-switchboard-firehose cmp-state put-fn msg-payload cmp-id))

(defn observe-state
  [{:keys [cmp-state put-fn msg-payload]}]
  (let [{:keys [from to]} msg-payload]
    (subscribe-comp-state cmp-state put-fn [from to])))

(defn send-to
  [{:keys [cmp-state msg-payload]}]
  (let [{:keys [to msg]} msg-payload
        dest-comp (to (:components @cmp-state))]
    (put! (:in-chan dest-comp) msg)))

(defn wire-all-out-channels
  "Function for calling the system-ready-fn on each component, which will pipe the channel used by
  the put-fn to the out-chan when the system is connected. Otherwise, messages sent before all
  channels are wired would get lost."
  [{:keys [cmp-state]}]
  (doseq [[_ cmp] (:components @cmp-state)]
    ((:system-ready-fn cmp))))

(def handler-map
  {:cmd/route              route-handler
   :cmd/route-all          route-all-handler
   :cmd/wire-comp          wire-comp
   :cmd/attach-to-firehose attach-to-firehose
   :cmd/self-register      self-register
   :cmd/observe-state      observe-state
   :cmd/send               send-to
   :status/system-ready    wire-all-out-channels})

(defn xform-fn
  "Transformer function for switchboard state snapshot. Allow serialization of snaphot for sending over WebSockets."
  [m]
  (let [xform (update-in m [:components] (fn [cmps] (into {} (mapv (fn [[k v]] [k k]) cmps))))]
    xform))

(defn component
  "Creates a switchboard component that wires individual components together into
  a communicating system."
  ([] (component :switchboard))
  ([switchboard-id]
   (let [switchboard (comp/make-component {:cmp-id            switchboard-id
                                           :state-fn          mk-state
                                           :handler-map       handler-map
                                           :opts              {:msgs-on-firehose false}
                                           :snapshot-xform-fn xform-fn})
         sw-in-chan (:in-chan switchboard)]
     (put! sw-in-chan [:cmd/self-register switchboard])
     switchboard)))

(defn send-cmd
  "Send message to the specified switchboard component."
  [switchboard cmd]
  (put! (:in-chan switchboard) cmd))

(defn send-mult-cmd
  "Send messages to the specified switchboard component."
  [switchboard cmds]
  (doseq [cmd cmds] (put! (:in-chan switchboard) cmd))
  (put! (:in-chan switchboard) [:status/system-ready]))
