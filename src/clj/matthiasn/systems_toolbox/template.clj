(ns matthiasn.systems-toolbox.template
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [clojure.core.match :refer [match]]
    [matthiasn.systems-toolbox.component :as comp]))

(defn mk-state
  "Return clean initial component state."
  [put-fn]
  (let [state (atom {})]
    state))

(defn in-handler
  "Handle incoming messages: process / add to application state."
  [state put-fn msg]
  (match msg
         [:cmd/some-msg-comp  m] ()
         :else (log/error "unknown msg in component" msg)))

(defn component
  [cmp-id]
  (comp/make-component cmp-id mk-state in-handler nil))
