(ns matthiasn.systems-toolbox.spec
  (:require  [expound.alpha :as exp]
    #?(:clj  [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s])
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])))

(defn valid-or-no-spec?
  "If spec exists, validate spec and warn if x is invalid, with detailed
   explanation. Also puts that information on firehose for use in inspect."
  ([spec x] (valid-or-no-spec? spec x nil))
  ([spec x firehose-put]
   (if (contains? (s/registry) spec)
     (if (s/valid? spec x)
       true
       (let [validation-error (exp/expound-str spec x)]
         (l/error validation-error)
         (when firehose-put (firehose-put {:spec-error validation-error}))
         false))
     (let [warning (str "UNDEFINED SPEC " spec)]
       (l/warn warning)
       (when firehose-put (firehose-put {:spec-warning warning}))
       true))))

(defn namespaced-keyword? [k] (and (keyword? k) (namespace k)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Message Spec
(s/def :systems-toolbox/msg-spec
  (s/or :no-payload (s/cat :msg-type namespaced-keyword?)
        :payload (s/cat :msg-type namespaced-keyword?
                        :msg-payload (s/or :map-payload map?
                                           :vector-payload vector?
                                           :nil-payload nil?
                                           :bool-payload boolean?
                                           :number-payload number?
                                           :string-payload string?
                                           :keyword-payload keyword?))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Firehose Spec
(s/def :st.firehose/cmp-id namespaced-keyword?)
(s/def :st.firehose/msg :systems-toolbox/msg-spec)
(s/def :st.firehose/ts pos-int?)
(s/def :st.firehose/snapshot map?)

(s/def :firehose/cmp-recv
  (s/keys :req-un [:st.firehose/cmp-id
                   :st.firehose/msg
                   :st.firehose/msg-meta
                   :st.firehose/ts]))

(s/def :firehose/cmp-put :firehose/cmp-recv)

(s/def :firehose/cmp-publish-state
  (s/keys :req-un [:st.firehose/cmp-id
                   :st.firehose/snapshot
                   :st.firehose/ts]))

(s/def :firehose/cmp-recv-state
  (s/keys :req-un [:st.firehose/cmp-id
                   :st.firehose/msg]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scheduler Spec
(s/def :st.schedule/timeout pos-int?)
(s/def :st.schedule/message :systems-toolbox/msg-spec)
(s/def :st.schedule/id keyword?)
(s/def :st.schedule/repeat boolean?)
(s/def :st.schedule/initial boolean?)

(s/def :cmd/schedule-new
  (s/keys :req-un [:st.schedule/timeout
                   :st.schedule/message]
          :opt-un [:st.schedule/id
                   :st.schedule/repeat
                   :st.schedule/initial]))

(s/def :cmd/schedule-delete
  (s/keys :req-un [:st.schedule/id]))

(s/def :info/deleted-timer keyword?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App State Specs
;; TODO: define specific specs for a component in order to validate the :new-state returned by handler functions
(s/def :app/state map?)

(s/def :cmd/publish-state nil?)
