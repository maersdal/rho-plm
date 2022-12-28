(ns eplme.backend.test-db-unit-graphs
  (:require [clojure.edn :as edn]
            [eplme.backend.component-graph :refer [create-graph-of
                                                   get-edit-points-from-graph
                                                   make-design-graph render-design-graph-history]]
            [eplme.backend.db-primitives :refer [find-excludes graph-ids]]
            [hyperfiddle.rcf :refer [tests]]
            [mount.core :as mount]
            [taoensso.tufte :as tufte]
            [ubergraph.core :as uber]
            [xtdb.api :as xt]
            [com.brunobonacci.mulog :as u]
            [eplme.backend.test-common :refer [node]]))


(comment 
  "Questions like:"
  "What units, where"
  "What units with what firmware?"
  "What configurations are currently deployed?"
  "What is the firmware upgrade rate of the system?"
  )

(xt/q (xt/db node)
      {:find ['(pull ?e [*])]
        :where [['?e :units '_]]})

(xt/q (xt/db node)
      {:find ['(pull ?e [:xt/id :site :location])]
       :where [['?e :units '_]]})
;; Note to self, oh this is powerful
;; shows the firmwares deployed
(xt/q (xt/db node)
      '{:find [?e ?fw ?fwname ?t ?te]
        :where [[?e :units _]
                [?e :firmware-sha ?fw]
                [?fw :name ?fwname]
                [(get-start-valid-time ?fw) ?t]
                [(get-start-valid-time ?e) ?te]]})

(defn all-units [node]
  (sort (flatten (seq (xt/q (xt/db node)
                            '{:find [?e]
                              :where [[?e :units _]]})))))

;; firmware upgrade rate per unit
(let [edit-times (sort (seq (set (for [e (all-units node)
                                       h (xt/entity-history (xt/db node) e :desc)]
                                   (::xt/valid-time h)))))
      edit-documents (for [edit-time edit-times]
                       (let [db (xt/db node edit-time)]
                         (xt/q db
                               '{:find [?e ?fw ?fwname ?t ?te]
                                 :where [[?e :units _]
                                         [?e :firmware-sha ?fw]
                                         [?fw :name ?fwname]
                                         [(get-start-valid-time ?fw) ?t]
                                         [(get-start-valid-time ?e) ?te]]})))]
  (reduce 
   (fn [result edits]
     (reduce (fn )))
   #{}
   edit-documents))

(comment 
  (mount/stop)
  (mount/start)
  )