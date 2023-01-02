(ns eplme.db.config
  (:require
   [clojure.edn]
   [mount.core :as mount :refer [defstate]]))

(defstate xtdb-settings
  :start (clojure.edn/read-string (slurp "src/config/xtdb_lmdb.edn")))

(comment 
  (mount/start)
  (mount/stop)
  (ns-unmap 'eplme.backend.db-config 'xtdb-settings)
  (ns-unmap 'eplme.backend.test-common 'node)
  (ns-unmap 'eplme.backend.db-primitives 'db)
  xtdb-settings
  )