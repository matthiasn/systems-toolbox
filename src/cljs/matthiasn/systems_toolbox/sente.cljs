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
           :else ())))

(defn mk-state
  "Return clean initial component state atom."
  [put-fn]
  (let [ws (sente/make-channel-socket! "/chsk" {:type :auto})]
    (sente/start-chsk-router! (:ch-recv ws) (make-handler put-fn))
    ws))

(defn all-msgs-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg-type msg-meta msg-payload]}]
  (let [ws cmp-state
        state (:state ws)
        send-fn (:send-fn ws)
        msg-w-ser-meta (assoc-in (merge msg-meta {}) [:sente-uid] (:uid @state))]
    (send-fn [msg-type {:msg msg-payload :msg-meta msg-w-ser-meta}])))

(defn component
  "Creates client-side WebSockets communication component."
  [cmp-id]
  (comp/make-component {:cmp-id   cmp-id
                        :state-fn mk-state
                        :all-msgs-handler all-msgs-handler
                        :opts     {:watch :state
                                   :reload-cmp false}}))
