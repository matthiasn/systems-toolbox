(ns matthiasn.systems-toolbox.perf-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [matthiasn.systems-toolbox.runtime-perf-test]))

(doo-tests 'matthiasn.systems-toolbox.runtime-perf-test)
