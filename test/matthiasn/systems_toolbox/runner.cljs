(ns matthiasn.systems-toolbox.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [matthiasn.systems-toolbox.component-test]
            [matthiasn.systems-toolbox.system-test]
            [matthiasn.systems-toolbox.scheduler-test]
            [matthiasn.systems-toolbox.log :as l]))

(l/enable-debug-log!)

(doo-tests 'matthiasn.systems-toolbox.component-test
           'matthiasn.systems-toolbox.system-test
           'matthiasn.systems-toolbox.scheduler-test)
