(ns matthiasn.systems-toolbox.sente
  (:require [cljs.core.match :refer-macros [match]]
            [matthiasn.systems-toolbox.component :as comp]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn deserialize-meta
  [payload]
  (let [[cmd-type {:keys [msg msg-meta]}] payload]
    (with-meta [cmd-type msg] msg-meta)))

(defn make-handler
  "Create handler function for messages from WebSocket connection. Calls put-fn with received
   messages."
  [put-fn]
  (fn [{:keys [event]}]
    (match event
           [:chsk/state {:first-open? true}] (put-fn [:first-open true])
           [:chsk/recv payload] (put-fn (deserialize-meta payload))
           [:chsk/handshake _] ()
           :else (println "Unmatched event in WS component:" event))))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [ws (sente/make-channel-socket! "/chsk" {:type :auto})]
    (sente/start-chsk-router! (:ch-recv ws) (make-handler put-fn))
    ws))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [ws _ msg]
  (let [state (:state ws)
        send-fn (:send-fn ws)
        [cmd-type payload] msg
        msg-meta (-> (merge (meta msg) {})
                     (assoc-in, [:sente-uid] (:uid @state)))]
    (send-fn [cmd-type {:msg payload :msg-meta msg-meta}])))

(defn component [cmp-id] (comp/make-component cmp-id mk-state in-handler nil))
