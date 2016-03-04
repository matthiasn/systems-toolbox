(ns matthiasn.systems-toolbox.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [matthiasn.systems-toolbox.component-test]
            [matthiasn.systems-toolbox.system-test]
            [matthiasn.systems-toolbox.scheduler-test]))

(doo-tests 'matthiasn.systems-toolbox.component-test
           'matthiasn.systems-toolbox.system-test
           'matthiasn.systems-toolbox.scheduler-test)
