(ns matthiasn.systems-toolbox.helpers)

(defn by-id
  "Helper function, gets DOM element by ID."
  [id]
  (.getElementById js/document id))
