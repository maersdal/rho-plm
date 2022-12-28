(ns eplme.backend.graph-common 
  (:require [ubergraph.core :as uber]
            [xtdb.api :as xt]))


(defn remove-internal-keys [doc]
  (dissoc doc :children :leaf :excludes))
(defn populate-metadata-on-graph [graph db]
  (reduce (fn [g n]
            (uber/add-attrs g n (remove-internal-keys (xt/entity db n))))
          graph
          (uber/nodes graph)))