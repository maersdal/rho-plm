;; This test runner is intended to be run from the command line
(ns eplme.test-runner
  (:require
    ;; require all the namespaces that you want to test
    [eplme.frontend.rhoplm-test]
    [figwheel.main.testing :refer [run-tests-async]]))

(defn -main [& args]
  (run-tests-async 5000))
