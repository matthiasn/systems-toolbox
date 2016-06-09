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
                                            :bool-payload boolean?))))
(s/def :systems-toolbox/msg message-spec)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scheduler Spec
(s/def :st.schedule/timeout pos-int?)
(s/def :st.schedule/message message-spec)
(s/def :st.schedule/repeat boolean?)
(s/def :st.schedule/initial boolean?)

(s/def :cmd/schedule-new
  (s/keys :req-un [:st.schedule/timeout
                   :st.schedule/message]
          :opt-un [:st.schedule/repeat
                   :st.schedule/initial]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec for :cmd/init-comp
(s/def :st.switchboard/cmp-id namespaced-keyword?)
(s/def :st.switchboard/state-fn fn?)
(s/def :st.switchboard/handler-map (s/nilable (s/map-of namespaced-keyword? fn?)))
(s/def :st.switchboard/all-msgs-handler fn?)
(s/def :st.switchboard/state-pub-handler (s/nilable fn?))
(s/def :st.switchboard/observed-xform (s/nilable fn?))
(s/def :st.switchboard/opts map?)

(s/def :cmd/init-comp
  (s/keys :req-un [:systems-toolbox.switchboard/cmp-id]
          :opt-un [:systems-toolbox.switchboard/state-fn
                   :systems-toolbox.switchboard/handler-map
                   :systems-toolbox.switchboard/all-msgs-handler
                   :systems-toolbox.switchboard/state-pub-handler
                   :systems-toolbox.switchboard/observed-xform
                   :systems-toolbox.switchboard/opts]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec for :cmd/route
(s/def :switchboard.route/from (s/or :single namespaced-keyword?
                                     :multiple (s/+ namespaced-keyword?)))
(s/def :switchboard.route/to namespaced-keyword?)
(s/def :switchboard.route/only namespaced-keyword?)
(s/def :switchboard.route/pred fn?)

(s/def :cmd/route
  (s/keys :req-un [:switchboard.route/from
                   :switchboard.route/to]
          :opt-un [:switchboard.route/only
                   :switchboard.route/pred]))

(s/def :cmd/route-all
  (s/keys :req-un [:switchboard.route/from
                   :switchboard.route/to]
          :opt-un [:switchboard.route/pred]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec for :cmd/observe-state
(s/def :switchboard.observe/from namespaced-keyword?)
(s/def :switchboard.observe/to (s/or :single namespaced-keyword?
                                     :multiple (s/+ namespaced-keyword?)))
(s/def :cmd/observe-state
  (s/keys :req-un [:switchboard.observe/from
                   :switchboard.observe/to]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec for :cmd/send
(s/def :st.switchboard-send/to namespaced-keyword?)
(s/def :st.switchboard-send/msg message-spec)
(s/def :cmd/send
  (s/keys :req-un [:st.switchboard-send/to
                   :st.switchboard-send/msg]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc Switchboard Specs
;; TODO: define structure of component map. Here, the switchboard is passed.
(s/def :cmd/self-register map?)
