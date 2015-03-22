(ns matthiasn.systems-toolbox.sente
  (:gen-class)
  (:require [clojure.core.match :refer [match]]
            [matthiasn.systems-toolbox.component :as comp]
            [taoensso.sente :as sente]
            [clojure.string :as str]
            [ring.middleware.defaults]
            [compojure.core :refer (defroutes GET POST)]
            [compojure.route :as route]
            [hiccup.core :as hiccup]
            [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
            [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
            [org.httpkit.server :as http-kit]
            [taoensso.sente.server-adapters.http-kit :refer (sente-web-server-adapter)]
            [taoensso.sente.packers.transit :as sente-transit]))

(defn make-handler
  "Create handler function for messages from WebSocket connection. Calls put-fn with received
   messages."
  [put-fn]
  (fn [{:keys [event]}]
    (match event
           [:chsk/state {:first-open? true}] (put-fn [:first-open true])
           [:chsk/recv payload]              (put-fn payload)
           [:chsk/handshake _]               ()
           :else (print "Unmatched event: %s" event))))

(defn make-state
  "Return clean initial component state atom."
  [put-fn]
  (let [ws (sente/make-channel-socket! "/chsk" {:type :auto})]
    (sente/start-chsk-router! (:ch-recv ws) (make-handler put-fn))
    ws))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [ws _ [cmd-type payload]]
  (let [state (:state ws)]
    ((:send-fn ws) [cmd-type (assoc payload :uid (:uid @state))])))

(defn component
  []
  (comp/make-component make-state in-handler nil {:atom false}))
