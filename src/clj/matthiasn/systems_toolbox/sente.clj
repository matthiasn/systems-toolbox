(ns matthiasn.systems-toolbox.sente
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [org.httpkit.server :as http-kit-server]
    [ring.middleware.defaults]
    [ring.util.response :refer [resource-response response content-type]]
    [compojure.core :refer (defroutes GET POST)]
    [compojure.route :as route]
    [matthiasn.systems-toolbox.component :as comp]
    [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
    [clojure.core.async :refer [<! chan put! mult tap pub sub timeout go-loop sliding-buffer]]
    [taoensso.sente :as sente]))

(def ring-defaults-config (assoc-in ring.middleware.defaults/site-defaults [:security :anti-forgery]
                                    {:read-token (fn [req] (-> req :params :csrf-token))}))

(defn user-id-fn
  "generates unique ID for request"
  [req]
  (let [uid (str (java.util.UUID/randomUUID))]
    (log/info "Connected:" (:remote-addr req) uid)
    uid))

(defn make-handler
  "Create handler function for messages from WebSocket connection. Calls put-fn with received messages."
  [_ put-fn]
  (fn [{:keys [event]}]
    (let [[cmd-type {:keys [msg msg-meta]}] event]
      (put-fn (with-meta [cmd-type msg] msg-meta)))))

(defn mk-state
  "Return clean initial component state atom."
  [index-page-fn port]
  (fn [put-fn]
    (let [ws (sente/make-channel-socket! sente-web-server-adapter {:user-id-fn user-id-fn})
          {:keys [ch-recv ajax-get-or-ws-handshake-fn ajax-post-fn]} ws]
      (defroutes my-routes
                 (GET "/" [] (content-type (response (index-page-fn false)) "text/html"))
                 (GET "/dev" [] (content-type (response (index-page-fn true)) "text/html"))
                 (GET "/chsk" req (ajax-get-or-ws-handshake-fn req))
                 (POST "/chsk" req (ajax-post-fn req))
                 (route/resources "/")
                 (route/not-found "Page not found"))
      (let [my-ring-handler (ring.middleware.defaults/wrap-defaults my-routes ring-defaults-config)
            server (http-kit-server/run-server my-ring-handler {:port port})]
        (log/info "Http-kit server is running on port" (:local-port (meta server))))
      (sente/start-chsk-router! ch-recv (make-handler ws put-fn))
      ws)))

(defn all-msgs-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg-type msg-meta msg-payload]}]
  (let [ws cmp-state
        chsk-send! (:send-fn ws)
        connected-uids (:connected-uids ws)
        dest-uid (:sente-uid msg-meta)
        msg-w-ser-meta [msg-type {:msg msg-payload :msg-meta msg-meta}]]
    (if dest-uid
      (chsk-send! dest-uid msg-w-ser-meta)
      (doseq [uid (:any @connected-uids)]
        (chsk-send! uid msg-w-ser-meta)))))

(defn component
  "Creates server-side WebSockets communication component."
  [cmp-id index-page-fn port]
  (comp/make-component {:cmp-id   cmp-id
                        :state-fn (mk-state index-page-fn port)
                        :all-msgs-handler all-msgs-handler
                        :opts     {:watch :connected-uids}}))
