(ns eplme.backend.component-graph
  (:require [clojure.set :as set]
            [eplme.backend.db-primitives :refer [graph-ids]]
            [mount.core :as mount]
            [taoensso.tufte :as tufte]
            [ubergraph.alg :as uber-alg]
            [ubergraph.core :as uber]
            [xtdb.api :as xt]
            [clojure.edn :as edn]))

(defn is-subgraph?
  "Is `g-check` contained in `g-ref`?"
  [g-ref g-check]
  (let [n0 (set (uber/nodes g-ref))
        n1 (set (uber/nodes g-check))
        e0 (set (map #(dissoc % :id) (uber/edges g-ref)))
        e1 (set (map #(dissoc % :id) (uber/edges g-check)))]
    ;; SUFFICIENT?
    (and (set/subset? n1 n0)
         (set/subset? e1 e0))))

(defn get-edit-points-from-graph
  "Given a xt db node `node` and a graph `g` which has node ids as xtdb ids, return the distinct timestamps from where any
   node in that graph has been edited"
  [db g]
  (->>
   (mapcat (partial xt/entity-history db) (uber/nodes g) (repeat :desc))
   (map ::xt/valid-time)
   distinct
   sort))

(defn create-graph-of
  ([db opts]
   (let [{:keys [comp-param] :or {comp-param [:component :r2]}} opts
         nodes (reduce concat (xt/q db {:find ['e]
                                        :where [['e :xt/id]
                                                (into ['e] comp-param)]}))
         edges (seq (xt/q db (update graph-ids :where (fn [x] (vec (conj x (into ['parent] comp-param)))))))
         nodes-with-inferred (seq (set (flatten edges)))
         filtered-edges (filter (fn [[a b]]
                                  (and (contains? (set nodes) a)
                                       (contains? (set nodes) b)))
                                edges)]
     (-> (uber/graph)
         (uber/add-nodes* nodes-with-inferred)
         (uber/add-directed-edges* filtered-edges)))))

(defn make-component-graph [db component-type]
  (create-graph-of db {:comp-param [:component component-type]}))

(defn get-nodes-of-type [db comp-type]
  (xt/q db '{:find [e]
             :in [comp-t]
             :where [[e :component comp-t]]}
        comp-type))


(defn render-component-graph-history [node comp-type & opts]
  (let [db (xt/db node) 
        comp-nodes (get-nodes-of-type db comp-type)
        story-points (->>
                      (mapcat (partial xt/entity-history db) (flatten (seq comp-nodes)) (repeat :desc))
                      (map ::xt/valid-time)
                      distinct
                      sort)
        {:keys [output]
         :or {output "./output/graphs/"}} opts]
    (->>
     story-points
     (map-indexed (fn [idx t] {:n idx
                               :graph (make-component-graph (xt/db node t) comp-type)}))
     (map (fn [{:keys [graph n]}]
            (uber/viz-graph
             graph
             {:save {:format :png
                     :filename  (str output "g_" (format "%03d" n) ".png")}}))))))