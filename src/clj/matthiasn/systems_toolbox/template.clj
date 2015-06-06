(ns matthiasn.systems-toolbox.template
  (:gen-class)
  (:require [matthiasn.systems-toolbox.component :as comp]))

(defn mk-state
  "Return clean initial component state. Map can hold whatever is useful inside this particular component."
  [put-fn]
  (let [state (atom {})]
    state))

(defn some-msg-handler
  "Handle incoming messages of the type chosen below in :handler-map. Any kind of behavior of a component can be
  implemented here."
  [{:keys [cmp-state msg msg-type msg-meta msg-payload cmp-id cfg]}]
  ())          ;; do something here

(defn component
  "Creates a component that has initial state created by the mk-state function, come component ID and reacts to
  the message types defined in :handler-map."
  [cmp-id]
  (comp/make-component {:cmp-id   cmp-id
                        :state-fn mk-state
                        :handler-map  {:cmd/some-msg-type some-msg-handler}}))
