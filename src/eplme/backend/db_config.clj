(ns eplme.backend.db-config
  (:require
   [clojure.edn]
   [mount.core :as mount :refer [defstate]]))

(defstate xtdb-settings
  :start (clojure.edn/read-string (slurp "src/config/xtdb_lmdb.edn")))

(comment 
  (mount/start)
  xtdb-settings
  )