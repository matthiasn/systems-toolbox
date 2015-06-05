(ns matthiasn.systems-toolbox.scheduler
  #+clj (:gen-class)
  #+cljs (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require
    #+clj [clojure.core.match :refer [match]]
    #+cljs [cljs.core.match :refer-macros [match]]
    [matthiasn.systems-toolbox.component :as comp]
    #+clj [clojure.core.async :refer [<! go-loop timeout]]
    #+cljs [cljs.core.async :refer [<! timeout]]))

;;; Systems Toolbox - Scheduler Subsystem

;;; This namespace describes a component / subsystem for scheduling the sending of messages that can then elsewhere
;;; trigger some action.

;;; Example: we want to let web clients know how many documents we have in a database so they can update the UI
;;; accordingly. The subsystem handling the database connectivity has the logic for figuring out how many documents
;;; there are when receiving a request, but no notion of repeatedly emitting this information itself. Now say we want
;;; this every 10 seconds. We tell the scheduler to emit the message type that will trigger the request every 10 seconds,
;;; and that's it.

;;; Internally, each scheduled event starts a go-loop with a timeout of the specified duration while recording
;;; the scheduled event in the state atom. Post-timeout, it is checked if the message is still scheduled to be sent
;;; and if so, the specified message is sent.

;;; Scheduled events can be deleted. TODO: implement

;;; TODO: record start time so that the scheduled time can be shown in UI. Platform-specific implementation.

;;; WARNING: timeouts specified here are not precise unless proven otherwise. Even if timeouts happen to have a
;;; sufficiently precise duration, the go-loop in which they run (and the associated thread pool) may be busy
;;; otherwise and delay the next iteration.

(defn start-loop
  "Starts a loop for sending messages at set intervals."
  [{:keys [cmp-state put-fn msg-payload]}]
  (let [timout-ms (:timeout msg-payload)
        scheduler-id (:id msg-payload)
        msg-to-send (:message msg-payload)]
    (when scheduler-id (swap! cmp-state assoc-in [:active-timers scheduler-id] msg-payload)
                       (println "Scheduling:" msg-payload))
    (go-loop []
      (<! (timeout timout-ms))
      (let [state @cmp-state
            active-timer (get (:active-timers state) scheduler-id)]
        (put-fn msg-to-send)
        (when active-timer
          (if (:repeat active-timer)
            (recur)
            (do
              (swap! cmp-state update-in [:active-timers] (dissoc (:active-timers state) scheduler-id))
              (put-fn [:info/completed-timer scheduler-id]))))))))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (atom {:active-timers {}}))

(defn component
  [cmp-id]
  (comp/make-component {:cmp-id   cmp-id
                        :state-fn mk-state
                        :handler-map {:cmd/schedule-new start-loop
                                      :cmd/schedule-delete ()}}))
