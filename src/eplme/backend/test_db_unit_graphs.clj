(ns eplme.backend.test-db-unit-graphs
  (:require [eplme.backend.common :refer [average diff1d-m ms->days]]
            [eplme.backend.fnschemas :refer [check-xt-db check-xt-node]]
            [eplme.backend.test-common :refer [node]]
            [eplme.backend.time-helper :refer [dt]]
            [mount.core :as mount]
            [xtdb.api :as xt]))

(mount/start)
(comment 
  "Questions like:"
  "What units, where"
  "What units with what firmware?"
  "What configurations are currently deployed?"
  "What is the firmware upgrade rate of the system?"
  )

(defn all-units-with-doc [db]
  {:pre [(check-xt-db db)]}
  (xt/q db
        {:find ['(pull ?e [*])]
         :where [['?e :units '_]]}))

(defn site-location-units [db]
  {:pre [(check-xt-db db)]}
  (xt/q db
        {:find ['(pull ?e [:xt/id :site :location])]
         :where [['?e :units '_]]}))
;; Note to self, oh this is powerful
;; shows the firmwares deployed
(defn units-with-update-and-firmware-time [db]
  {:pre [(check-xt-db db)]}
  (xt/q db
        '{:find [?e ?fw ?fwname ?t ?te]
          :where [[?e :units _]
                  [?e :firmware-sha ?fw]
                  [?fw :name ?fwname]
                  [(get-start-valid-time ?fw) ?t]
                  [(get-start-valid-time ?e) ?te]]}))

(defn all-units [node]
  {:pre [(check-xt-node node)]}
  (sort (flatten (seq (xt/q (xt/db node)
                            '{:find [?e]
                              :where [[?e :units _]]})))))


(defn all-edit-documents-historical [node]
  {:pre [(check-xt-node node)]}
  (let [edit-times (sort (seq (set (for [e (all-units node)
                                         h (xt/entity-history (xt/db node) e :desc)]
                                     (::xt/valid-time h)))))
        edit-documents (mapcat #(seq (units-with-update-and-firmware-time (xt/db node %))) edit-times)]
    edit-documents))
;; change rate per unit, or churn
(defn calculate-churn-for-all [node]
  (let [diff (diff1d-m dt)
        resultmaps (vals (reduce
                          (fn [r [id hash name t-fw t-unit]]
                            (update r id (fn [m]
                                           (-> m
                                               ;; wrong ?
                                               (assoc :id id)
                                               (update :hash conj hash)
                                               (update :name conj name)
                                               (update :diffu diff id t-unit)
                                               (update :tfw conj t-fw)
                                               (update :tu conj t-unit)))))
                          {}
                          (all-edit-documents-historical node)))]
    (map (fn [m]
           (if-let [v (:diffu m)] 
             (assoc m :churn-rate (ms->days (average v)))
             m))
         resultmaps)))
(comment 
  (require '[criterium.core :as crit])
  (all-edit-documents-historical node)
  (calculate-churn-for-all node)
(crit/with-progress-reporting
  (crit/quick-bench
   (calculate-churn-for-all node)))
  )

(comment 
  
  (mount/stop)
  (mount/start)
  )