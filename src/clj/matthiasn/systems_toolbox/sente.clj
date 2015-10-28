(ns matthiasn.systems-toolbox.sente
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [ring.middleware.defaults]
    [ring.util.response :refer [resource-response response content-type]]
    [compojure.core :refer (defroutes GET POST)]
    [compojure.route :as route]
    [matthiasn.systems-toolbox.component :as comp]
    [clojure.core.async :refer [<! chan put! mult tap pub sub timeout go-loop sliding-buffer]]
    [immutant.web :as immutant]
    [taoensso.sente :as sente]
    [taoensso.sente.server-adapters.immutant :refer (sente-web-server-adapter)]
    [taoensso.sente.packers.transit :as sente-transit]))

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

(def host (get (System/getenv) "HOST" "localhost"))
(def port (get (System/getenv) "PORT" "8888"))

(defn mk-state
  "Return clean initial component state atom."
  [index-page-fn]
  (fn [put-fn]
    (let [ws (sente/make-channel-socket! sente-web-server-adapter {:user-id-fn user-id-fn
                                                                   :packer (sente-transit/get-flexi-packer :edn)})
          {:keys [ch-recv ajax-get-or-ws-handshake-fn ajax-post-fn]} ws]
      (defroutes my-routes
                 (GET "/" [] (content-type (response (index-page-fn false)) "text/html"))
                 (GET "/chsk" req (ajax-get-or-ws-handshake-fn req))
                 (POST "/chsk" req (ajax-post-fn req))
                 (route/resources "/")
                 (route/not-found "Page not found"))
      (let [my-ring-handler (ring.middleware.defaults/wrap-defaults my-routes ring-defaults-config)
            server (immutant/run my-ring-handler :host host :port port)]
        (log/info "Immutant-web is listening on port" port "on interface" host))
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

(defn cmp-map
  "Creates server-side WebSockets communication component map."
  {:added "0.3.1"}
  [cmp-id index-page-fn]
  {:cmp-id           cmp-id
   :state-fn         (mk-state index-page-fn)
   :all-msgs-handler all-msgs-handler
   :opts             {:watch :connected-uids
                      :snapshots-on-firehose false}})

(defn component
  "Creates server-side WebSockets communication component map. Unlike in other components, this function is still
  required when using the component as in the sample application, as the application would otherwise almost
  immediately exit."
  [cmp-id index-page-fn]
  (comp/make-component (cmp-map cmp-id index-page-fn)))
