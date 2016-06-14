(ns matthiasn.systems-toolbox.spec
  (:require
    #?(:clj  [clojure.spec :as s]
       :cljs [cljs.spec :as s])
    #?(:clj  [clojure.tools.logging :as l]
       :cljs [matthiasn.systems-toolbox.log :as l])))

#?(:cljs (defn boolean? [x] (= (type x) js/Boolean)))

(defn valid-or-no-spec?
  "If spec exists, validate spec and warn if x is invalid, with detailed explanation.
  If spec does not exist, log warning."
  [spec x]
  (if (contains? (s/registry) spec)
    (if (s/valid? spec x)
      true
      (do (l/warn "VALIDATION FAILED for" spec "," (s/explain-str spec x)) false))
    (if x  ; only check for spec when x is truthy
      (do (l/warn (str "UNDEFINED SPEC " spec)) true)
      true)))

(defn pos-int? [n] (and (integer? n) (pos? n)))

(defn namespaced-keyword? [k] (and (keyword? k) (namespace k)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Message Spec
(s/def :systems-toolbox/msg-spec
  (s/or :no-payload (s/cat :msg-type namespaced-keyword?)
        :payload (s/cat :msg-type namespaced-keyword?
                        :msg-payload (s/alt :map-payload map?
                                            :nil-payload nil?
                                            :bool-payload boolean?
                                            :number-payload number?
                                            :keyword-payload keyword?))))

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
