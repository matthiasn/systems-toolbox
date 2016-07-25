(ns example.server-switchboard
  (:require
    [matthiasn.systems-toolbox.switchboard :as sb]
    [matthiasn.systems-toolbox.scheduler :as sched]
    [example.pointer :as ptr]))

(defonce switchboard (sb/component :server/switchboard))

(defn restart!
  "Starts or restarts system by asking switchboard to fire up the provided
   ws-cmp, a scheduler component and the ptr component, which handles and counts
   messages about mouse moves."
  [ws-cmp]
  (sb/send-mult-cmd
    switchboard
    [ws-cmp
     [:cmd/init-comp (sched/cmp-map :server/scheduler-cmp)]
     [:cmd/init-comp (ptr/cmp-map :server/ptr-cmp)]
     [:cmd/route-all {:from :server/ptr-cmp :to :server/ws-cmp}]
     [:cmd/route {:from :server/ws-cmp :to :server/ptr-cmp}]]))
