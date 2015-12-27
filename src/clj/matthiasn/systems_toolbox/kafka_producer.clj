(ns matthiasn.systems-toolbox.kafka-producer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [clj-kafka.new.producer :as kp]
    [taoensso.nippy :as nippy]))

(def kafka-address (get (System/getenv) "KAFKA_ADDRESS" "127.0.0.1:9092"))
(def kafka-producer-id (get (System/getenv) "KAFKA_PRODUCER_ID" "producer-cmp"))

(defn kafka-producer-state-fn
  "Returns initial component state function. Calling this function will return the
  initial component state containing the Kafka producer and the config.."
  [cfg]
  (fn
    [put-fn]
    (let [producer-id (or (:producer-id cfg) kafka-producer-id)
          prod (kp/producer {"bootstrap.servers" kafka-address
                             "client-id"         producer-id}
                            (kp/byte-array-serializer)
                            (kp/byte-array-serializer))]
      {:state (atom {:producer prod
                     :cfg cfg})})))

(defn all-msgs-handler
  "Publishes messages on Kafka topic when the msg-type-to-topic mapping contains
  the msg-type. Messages on the topic contain metadata for the systems-toolbox
  message and are serialized using Nippy."
  [{:keys [cmp-state msg-type msg-meta msg-payload]}]
  (let [state-snapshot @cmp-state]
    (future
      (kp/send
        (:producer state-snapshot)
        (let [kafka-topic (msg-type (:msg-type-topic-mapping (:cfg state-snapshot)))
              serialized (nippy/freeze {:msg-type    msg-type
                                        :msg-meta    msg-meta
                                        :msg-payload msg-payload})]
          (if kafka-topic
            (kp/record kafka-topic serialized)
            (log/warn "No Kafka topic specified for msg type:" msg-type)))
        (fn [m err]
          (when err
            (log/info "producer future err:" err m)))))))

(defn cmp-map
  "Create Kafka producer component, which sends serialized message on the topic associated
  with a particular message type. The mappings are passed inside the cfg argument like this:

      {:msg-type-topic-mapping {:inspect/probe \"inspect-probe-events\"}}

  Messages received by this component will only be published on a Kafka topic when there is
  a specified topic for the message type. Otherwise, a warning will be logged."
  {:added "0.4.9"}
  [cmp-id cfg]
  {:cmp-id           cmp-id
   :state-fn         (kafka-producer-state-fn cfg)
   :all-msgs-handler all-msgs-handler
   :opts             {:msgs-on-firehose      false
                      :snapshots-on-firehose false}})
