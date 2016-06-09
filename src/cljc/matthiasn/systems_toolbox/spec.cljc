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
(def message-spec
  (s/or :no-payload (s/cat :msg-type namespaced-keyword?)
        :payload (s/cat :msg-type namespaced-keyword?
                        :msg-payload (s/alt :map-payload map?
                                            :nil-payload nil?
                                            :bool-payload boolean?
                                            :keyword-payload keyword?
                                            :fn-payload fn?))))
(s/def :systems-toolbox/msg message-spec)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scheduler Spec
(s/def :st.schedule/timeout pos-int?)
(s/def :st.schedule/message message-spec)
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
;; Spec for :cmd/init-comp
(s/def :st.switchboard/cmp-id namespaced-keyword?)
(s/def :st.switchboard/state-fn fn?)
(s/def :st.switchboard/handler-map (s/nilable (s/map-of namespaced-keyword? fn?)))
(s/def :st.switchboard/all-msgs-handler fn?)
(s/def :st.switchboard/state-pub-handler (s/nilable fn?))
(s/def :st.switchboard/observed-xform (s/nilable fn?))
(s/def :st.switchboard/opts map?)
(s/def :st.switchboard/state-spec namespaced-keyword?)

(s/def :cmd/init-comp
  (s/keys :req-un [:st.switchboard/cmp-id]
          :opt-un [:st.switchboard/state-fn
                   :st.switchboard/handler-map
                   :st.switchboard/all-msgs-handler
                   :st.switchboard/state-pub-handler
                   :st.switchboard/observed-xform
                   :st.switchboard/opts
                   :st.switchboard/state-spec]))

(s/def :st.switchboard/cmp namespaced-keyword?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec for :cmd/route
(s/def :st.switchboard.route/from (s/or :single :st.switchboard/cmp
                                        :multiple (s/+ :st.switchboard/cmp)))
(s/def :st.switchboard.route/to :st.switchboard/cmp)
(s/def :st.switchboard.route/only :st.switchboard/cmp)
(s/def :st.switchboard.route/pred fn?)

(s/def :cmd/route
  (s/keys :req-un [:st.switchboard.route/from
                   :st.switchboard.route/to]
          :opt-un [:st.switchboard.route/only
                   :st.switchboard.route/pred]))

(s/def :cmd/route-all
  (s/keys :req-un [:st.switchboard.route/from
                   :st.switchboard.route/to]
          :opt-un [:st.switchboard.route/pred]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec for :cmd/observe-state
(s/def :st.switchboard.observe/from :st.switchboard/cmp)
(s/def :st.switchboard.observe/to (s/or :single :st.switchboard/cmp
                                     :multiple (s/+ :st.switchboard/cmp)))
(s/def :cmd/observe-state
  (s/keys :req-un [:st.switchboard.observe/from
                   :st.switchboard.observe/to]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec for :cmd/send
(s/def :st.switchboard-send/to :st.switchboard/cmp)
(s/def :st.switchboard-send/msg message-spec)
(s/def :cmd/send
  (s/keys :req-un [:st.switchboard-send/to
                   :st.switchboard-send/msg]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc Switchboard Specs
;; TODO: define structure of component map. Here, the switchboard is passed.
(s/def :cmd/self-register map?)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; App State Specs
;; TODO: define specific specs for a component in order to validate the :new-state returned
;;       by handler functions
(s/def :app/state map?)
