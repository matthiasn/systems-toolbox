(ns matthiasn.systems-toolbox.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [matthiasn.systems-toolbox.component-test]
            [matthiasn.systems-toolbox.system-test]
            [matthiasn.systems-toolbox.scheduler-test]
            [matthiasn.systems-toolbox.runtime-perf-test]))

(doo-tests 'matthiasn.systems-toolbox.component-test
           'matthiasn.systems-toolbox.runtime-perf-test
           'matthiasn.systems-toolbox.system-test
           'matthiasn.systems-toolbox.scheduler-test)
