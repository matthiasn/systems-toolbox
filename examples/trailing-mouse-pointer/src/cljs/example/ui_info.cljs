(ns example.ui-info
  (:require [matthiasn.systems-toolbox-ui.reagent :as r]
            [re-frame.core :refer [subscribe]]
            [matthiasn.systems-toolbox-ui.helpers :refer [by-id]]))

(defn info-view
  "Show some info about app state, plus toggle buttons for showing all mouse
   positions, both local and from server."
  [put-fn]
  (let [from-server (subscribe [:from-server])
        rtt-times (subscribe [:rtt-times])
        local (subscribe [:local])]
    (fn [put-fn]
      (let [last-rt (:rt-time @from-server)
            rtt-times @rtt-times
            mx (apply max rtt-times)
            mn (apply min rtt-times)
            cnt (count rtt-times)
            mean (.round js/Math (if (seq rtt-times)
                                   (/ (apply + rtt-times) cnt)
                                   0))
            local-pos @local
            latency-string (str mean " mean / " mn " min / " mx " max / " last-rt
                                " last")]
        [:div
         [:strong "Mouse Moves Processed: "] cnt [:br]
         [:strong "Processed since Startup: "]
         (:count @from-server) [:br]
         [:strong "Current position: "] "x: " (:x local-pos) " y: " (:y local-pos)
         [:br]
         [:strong "Latency (ms): "] latency-string [:br]
         [:br]
         #_#_[:button {:on-click #(put-fn [:cmd/show-all :local])} "show all"]
             [:button {:on-click #(do (put-fn [:mouse/get-hist])
                                      (put-fn [:cmd/show-all :server]))}
              "show all (server)"]]))))
