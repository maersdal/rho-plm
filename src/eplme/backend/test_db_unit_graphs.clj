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
            [eplme.backend.time-helper :refer [dt]]
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

(defn ms->days [ms]
  (/ ms 
     (* 1000.0 3600 24)))
(defn average [xs]
  (/ (reduce + xs)
     (count xs)))

;; change rate per unit, or churn
;; this is better done with dataframes
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
                                         [(get-start-valid-time ?e) ?te]]})))
      get-unit-time (fn [[id hash name t-fw t-unit]]
                      [id t-unit])
      edit-times-per-unit (reduce
                           (fn [r [k v]]
                             (update r k conj v))
                           {}
                           (for [d edit-documents
                                 v d]
                             (get-unit-time v)))
      intervals (into {} (->> edit-times-per-unit
                              (map (fn [[k v]]
                                     [k  (reduce (let [prev (atom nil)]
                                                   (fn [r item]
                                                     (let [p @prev]
                                                       (reset! prev item)
                                                       (if (nil? p)
                                                         r
                                                         (conj r (dt item p)))))) [] v)]))))
      flattened (map (fn [[k v] [k2 v2]]
                       (assert (= k k2))
                       (merge {:id k :edit-times (vec v2)}
                              (when (seq v)
                                {:intervals v})))
                     (sort intervals)
                     (sort edit-times-per-unit))]
(->> flattened
     (map (fn [m]
            (if (seq (:intervals m))
              (assoc m :churn-rate (ms->days (average (:intervals m))))
              m)))))


(comment 
  (mount/stop)
  (mount/start)
  )