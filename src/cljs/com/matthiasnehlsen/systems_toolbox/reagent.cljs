(ns com.matthiasnehlsen.systems-toolbox.reagent
  (:require [reagent.core :as r :refer [atom]]
            [com.matthiasnehlsen.systems-toolbox.core :as comp]
            [cljs.core.async :refer [chan pub sub buffer sliding-buffer pipe]]))

(defn by-id
  "Helper function, gets DOM element by ID."
  [id]
  (.getElementById js/document id))

(defn mount-component
  "Mounts view-fn component. Takes put-fn as the function that can be called when some message
   needs to be sent back to the switchboard. Returns a function that handles incoming messages."
  [view-fn id put-fn]
  (let [app (atom {})]
    (r/render-component [view-fn app put-fn] (by-id id))
    (fn [[_ state-snapshot]]
      (reset! app state-snapshot))))

(defn init-component
  "Creates Reagent component with wired up channels."
  [view-fn state-pub state-in-chan id]
  (let [init-partial (partial mount-component view-fn id)
        cmpnt (comp/component-with-channels init-partial (sliding-buffer 1) (buffer 1))]
    (sub state-pub :app-state (:in-chan cmpnt))
    (pipe (:out-chan cmpnt) state-in-chan)))

(defn init-components
  "Creates Reagent components with wired up channels."
  [state-pub state-in-chan views]
  (doseq [[view id] views]
    (init-component view state-pub state-in-chan id)))
