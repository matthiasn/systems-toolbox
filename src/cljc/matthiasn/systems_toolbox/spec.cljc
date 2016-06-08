(ns matthiasn.systems-toolbox.spec
  (:require  [matthiasn.systems-toolbox.log :as l]
    #?(:clj  [clojure.spec :as s]
       :cljs [cljs.spec :as s])))

#?(:cljs (defn boolean? [x] (= (type x) js/Boolean)))

(defn valid-or-no-spec?
  "If spec exists, validate spec and warn if x is invalid, with detailed explanation.
  If spec does not exist, log deprecation warning."
  [spec x]
  (if (contains? (s/registry) spec)
    (if (s/valid? spec x) true
                          (l/warn "Could not validate" x "because" (s/explain spec x)))
    (if x  ; only check for spec when x is truthy
      (do (l/warn (str "undefined spec " spec))
          true)
      true)))

(defn pos-int? [n] (and (integer? n) (pos? n)))

(def message-spec
  (s/or :no-payload (s/cat :msg-type keyword?)
        :payload (s/cat :msg-type keyword?
                        :msg-payload (s/alt :map-payload map?
                                            :nil-payload nil?
                                            :bool-payload boolean?))))
(s/def :systems-toolbox/msg message-spec)

(s/def :st.schedule/timeout pos-int?)
(s/def :st.schedule/message message-spec)
(s/def :st.schedule/repeat boolean?)
(s/def :st.schedule/initial boolean?)

(s/def :cmd/schedule-new
  (s/keys :req-un [:st.schedule/timeout
                   :st.schedule/message]
          :opt-un [:st.schedule/repeat
                   :st.schedule/initial]))
