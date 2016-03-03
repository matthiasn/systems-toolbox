(ns matthiasn.systems-toolbox.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [matthiasn.systems-toolbox.test]
            [matthiasn.systems-toolbox.component-test]))

(doo-tests 'matthiasn.systems-toolbox.test
           'matthiasn.systems-toolbox.component-test)
