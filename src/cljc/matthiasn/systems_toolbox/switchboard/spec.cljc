(ns matthiasn.systems-toolbox.switchboard.spec
  (:require  [matthiasn.systems-toolbox.spec :as sts]
    #?(:clj  [clojure.spec :as s]
       :cljs [cljs.spec :as s])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec for :cmd/init-comp
(s/def :st.switchboard.init/cmp-id sts/namespaced-keyword?)
(s/def :st.switchboard.init/state-fn fn?)
(s/def :st.switchboard.init/handler-map (s/nilable (s/map-of sts/namespaced-keyword? fn?)))
(s/def :st.switchboard.init/all-msgs-handler fn?)
(s/def :st.switchboard.init/state-pub-handler (s/nilable fn?))
(s/def :st.switchboard.init/observed-xform (s/nilable fn?))
(s/def :st.switchboard.init/opts map?)
(s/def :st.switchboard.init/state-spec sts/namespaced-keyword?)

(s/def :st.switchboard.init/cmp-map
  (s/keys :req-un [:st.switchboard.init/cmp-id]
          :opt-un [:st.switchboard.init/state-fn
                   :st.switchboard.init/handler-map
                   :st.switchboard.init/all-msgs-handler
                   :st.switchboard.init/state-pub-handler
                   :st.switchboard.init/observed-xform
                   :st.switchboard.init/opts
                   :st.switchboard.init/state-spec]))

(s/def :cmd/init-comp (s/or :single-cmp :st.switchboard.init/cmp-map
                            :multiple-cmps (s/+ :st.switchboard.init/cmp-map)))

(s/def :st.switchboard/cmp sts/namespaced-keyword?)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Spec for :cmd/route
(s/def :st.switchboard.route/from (s/or :single :st.switchboard/cmp
                                        :multiple (s/+ :st.switchboard/cmp)))
(s/def :st.switchboard.route/to (s/or :single :st.switchboard/cmp
                                      :multiple (s/+ :st.switchboard/cmp)))
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

(s/def :cmd/attach-to-firehose sts/namespaced-keyword?)


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
(s/def :st.switchboard-send/msg :systems-toolbox/msg-spec)
(s/def :cmd/send
  (s/keys :req-un [:st.switchboard-send/to
                   :st.switchboard-send/msg]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Misc Switchboard Specs
;; TODO: define structure of component map. Here, the switchboard is passed.
(s/def :cmd/self-register map?)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Switchboard State Spec
(s/def :st.switchboard/components (s/map-of sts/namespaced-keyword? map?))
(s/def :st.switchboard.sub/from :st.switchboard/cmp)
(s/def :st.switchboard.sub/to :st.switchboard/cmp)
(s/def :st.switchboard.sub/msg-type sts/namespaced-keyword?)
(s/def :st.switchboard.sub/type #{:sub})

(s/def :st.switchboard/sub-map
  (s/keys :req-un [:st.switchboard.sub/from
                   :st.switchboard.sub/to
                   :st.switchboard.sub/msg-type
                   :st.switchboard.sub/type]))
(s/def :st.switchboard/subs (s/* :st.switchboard/sub-map))

(s/def :st.switchboard.fh-tap/type #{:fh-tap})
(s/def :st.switchboard/fh-tap-map
  (s/keys :req-un [:st.switchboard.sub/from
                   :st.switchboard.sub/to
                   :st.switchboard.fh-tap/type]))
(s/def :st.switchboard/fh-taps (s/* :st.switchboard/fh-tap-map))

(s/def :st.switchboard/state-spec
  (s/keys :req-un [:st.switchboard/components
                   :st.switchboard/subs
                   :st.switchboard/taps
                   :st.switchboard/fh-taps]))
