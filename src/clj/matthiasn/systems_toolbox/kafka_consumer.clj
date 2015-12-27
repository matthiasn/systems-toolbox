(ns matthiasn.systems-toolbox.kafka-consumer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [clj-kafka.consumer.zk :as kcz]
    [clj-kafka.admin :as admin]
    [taoensso.nippy :as nippy]))

(def zookeper-address (get (System/getenv) "ZOOKEEPER_ADDRESS" "127.0.0.1:2181"))
(def kafka-consumer-id (get (System/getenv) "KAFKA_CONSUMER_ID" "clj-kafka.consumer"))
(def kafka-port (get (System/getenv) "KAFKA_PORT" "9092"))

(def config
  {"zookeeper.connect"  zookeper-address
   "group.id"           kafka-consumer-id
   "port"               kafka-port
   "auto.offset.reset"  "smallest"
   "auto.commit.enable" "true"})

(defn kafka-consumer-state-fn
  "Returns function that creates the Kafka consumer component state state while using provided
  configuration.
  This component creates multiple listeners, one for each topic provided in the topic set
  from config. Note that messages taken off the topics need to be sent by the systems-toolbox,
  encoded by Nippy."
  [cfg]
  (fn
    [put-fn]
    (let [zk-client (admin/zk-client zookeper-address {:session-timeout-ms    500
                                                       :connection-timeout-ms 500})]
      (let [consumer (kcz/consumer config)
            topics (:topics cfg)]
        (doseq [topic topics]
          (when-not (admin/topic-exists? zk-client topic)
            (log/info "Created Kafka topic:" topic)
            (admin/create-topic zk-client topic))
          (log/info "Listening to Kafka topic:" topic)
          (future
            (let [messages (kcz/messages consumer topic)]
              (doseq [msg messages]
                (try
                  (let [thawed (nippy/thaw (:value msg))
                        msg-type (:msg-type thawed)
                        msg-payload (:msg-payload thawed)
                        msg-meta (:msg-meta thawed)]
                    (put-fn (with-meta [msg-type msg-payload] (or msg-meta {}))))
                  (catch Exception ex (log/error "Error while taking message off Kafka topic:" ex)))))))
        {:state (atom {:consumer consumer})}))))

(defn cmp-map
  "Creates Kafka consumer component.
  Inside the cfg parameter, a set of topics to listen to is specified, like this:

      {:topics #{some-topic-name-string another-topic-name-string}}

  This component is currently one-way as in not having a message handler. But it could
  make sense to for example listen to messages that ask for listening to additional
  topics, or for returning component stats."
  {:added "0.4.9"}
  [cmp-id cfg]
  {:cmp-id   cmp-id
   :state-fn (kafka-consumer-state-fn cfg)})
