(ns matthiasn.systems-toolbox.switchboard
  (:require  [matthiasn.systems-toolbox.component :as comp]
             [matthiasn.systems-toolbox.switchboard.route :as rt]
             [matthiasn.systems-toolbox.switchboard.init :as i]
    #?(:clj  [clojure.core.async :refer [put! chan pipe sub tap]]
       :cljs [cljs.core.async :refer [put! chan pipe sub tap]])
    #?(:clj  [clojure.pprint :as pp]
       :cljs [cljs.pprint :as pp])
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])
    #?(:clj  [clojure.spec :as s]
       :cljs [cljs.spec :as s])))

(defn subscribe
  "Subscribe component to a specified publisher."
  [{:keys [cmp-state from to msg-type pred]}]
  (let [app @cmp-state
        [from-cmp from-pub] from
        [to-cmp to-chan] to
        pub-comp (from-cmp (:components app))
        sub-comp (to-cmp (:components app))
        target-chan (if pred
                      (let [filtered-chan (chan 1 (filter pred))]
                        (pipe filtered-chan (to-chan sub-comp))
                        filtered-chan)
                      (to-chan sub-comp))]
    (sub (from-pub pub-comp) msg-type target-chan)
    (swap! cmp-state update-in [:subs] conj {:from from-cmp :to to-cmp :msg-type msg-type :type :sub})))

(defn subscribe-comp-state
  "Subscribe component to a specified publisher."
  [{:keys [cmp-state put-fn from to]}]
  (doseq [t (flatten [to])]
    (subscribe {:cmp-state cmp-state
                :put-fn    put-fn
                :from      [from :state-pub]
                :msg-type  :app/state
                :to        [t :sliding-in-chan]})))

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
  {:state (atom {:components {}
                 :subs #{}
                 :taps #{}
                 :fh-taps #{}})})

(defn attach-to-firehose
  "Attaches a component to firehose channel. For example for observational components."
  [{:keys [current-state msg-payload cmp-id]}]
  (let [to msg-payload
        sw-firehose-mult (:firehose-mult (cmp-id (:components current-state)))
        to-comp (to (:components current-state))
        error-log #(l/error "Could not create tap: " cmp-id " -> " to " - " %)]
    (try (do
           (tap sw-firehose-mult (:in-chan to-comp))
           {:new-state (update-in current-state [:fh-taps] conj {:from cmp-id :to to :type :fh-tap})})
         #?(:clj  (catch Exception e (error-log (.getMessage e)))
            :cljs (catch js/Object e (error-log e))))))

(defn observe-state
  [{:keys [cmp-state put-fn msg-payload]}]
  (let [{:keys [from to]} msg-payload]
    (subscribe-comp-state {:cmp-state cmp-state
                           :put-fn    put-fn
                           :from from
                           :to to})))

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
  {:cmd/route              rt/route-handler
   :cmd/route-all          rt/route-all-handler
   :cmd/wire-comp          (i/wire-or-init-comp false)
   :cmd/init-comp          (i/wire-or-init-comp true)
   :cmd/attach-to-firehose attach-to-firehose
   :cmd/self-register      self-register
   :cmd/observe-state      observe-state
   :cmd/send               send-to
   :status/system-ready    wire-all-out-channels})

(defn xform-fn
  "Transformer function for switchboard state snapshot. Allows serialization of snaphot for sending over WebSockets."
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
  (doseq [cmd cmds] (when cmd (put! (:in-chan switchboard) cmd)))
  (put! (:in-chan switchboard) [:status/system-ready]))
