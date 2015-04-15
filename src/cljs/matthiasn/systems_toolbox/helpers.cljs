(ns matthiasn.systems-toolbox.helpers)

(defn by-id
  "Helper function, gets DOM element by ID."
  [id]
  (.getElementById js/document id))

(defn now
  "Get formatted timestamp string for time of call."
  []
  (.toISOString (js/Date.)))
