(ns eplme.dev.test-common
  (:require [eplme.dev.test-loaders :refer
             [demo-components demo-firmware demo-meta demo-units]]
            [mount.core :refer [defstate]]
            [xtdb.api :as xt]))

(defstate node
  "Database connection - for now"
  :start (let [node (xt/start-node {})]
           (xt/submit-tx node demo-meta)
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
  (ns-unmap 'eplme.backend.test-loaders 'demo-components)
  (ns-unmap 'eplme.backend.test-loaders 'demo-units)
  
  
  )