(ns com.matthiasnehlsen.systems-toolbox.sente
  (:require [cljs.core.match :refer-macros [match]]
            [com.matthiasnehlsen.systems-toolbox.core :as comp]
            [taoensso.sente :as sente :refer (cb-success?)]
            [taoensso.sente.packers.transit :as sente-transit]))

(def packer
  "Defines our packing (serialization) format for client<->server comms."
  (sente-transit/get-flexi-packer :json))

(defn make-handler
  "Create handler function for messages from WebSocket connection. Calls put-fn with received
   messages."
  [put-fn]
  (fn [{:keys [event]}]
    (match event
           [:chsk/state {:first-open? true}] (put-fn [:first-open true])
           [:chsk/recv payload] (put-fn payload)
           :else (print "Unmatched event: %s" event))))

(defn init-component
  "Initializes Sente / WebSockets component. Takes put-fn as the function that can be called
   when some message needs to be sent back to the switchboard. Returns a function that handles
   incoming messages."
  [put-fn]
  (let [ws (sente/make-channel-socket! "/chsk" {:packer packer :type :auto})
        {:keys [ch-recv send-fn state]} ws]
    (sente/start-chsk-router! ch-recv (make-handler put-fn))
    (fn [[cmd-type payload]]
      (send-fn [cmd-type (assoc payload :uid (:uid @state))]))))

(defn component
  []
  (comp/component-single-in-single-out init-component
                                       {:in-chan [:buffer 1] :out-chan [:buffer 1]}))
