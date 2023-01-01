(ns eplme.backend.test-common
  (:require [mount.core :refer [defstate]]
            [eplme.backend.test-loaders :refer
             [demo-components demo-firmware demo-units]]
            [xtdb.api :as xt]))

(defstate node
  :start (let [node (xt/start-node {})]
             (xt/submit-tx node demo-components)
             (xt/submit-tx node demo-firmware)
             (xt/submit-tx node demo-units)
           node)
  :stop (.close node)
  )

(comment 
  (keys (ns-publics 'eplme.backend.test-loaders))
  (remove-ns 'eplme.backend.test-loaders)
  (load "test_loaders")
  (map #(ns-unmap *ns* %) (keys (ns-interns *ns*)))
  
  
  )