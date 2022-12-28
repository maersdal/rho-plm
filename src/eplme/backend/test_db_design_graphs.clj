(ns eplme.backend.test-db-design-graphs
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
            [eplme.backend.test-common :refer [node]])
(:import [java.security MessageDigest]))
(comment
  "Design graphs is per project"
  )

(mount/start)


(defn sha256 [string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))
;; GIT integration..uri


(xt/entity (xt/db node) :nrf9160-sparkfun)
(xt/entity (xt/db node) :computer-feather)

(find-excludes (xt/db node) :nrf9160-sparkfun)


(comment
  (def db (xt/db node))

  (uber/pprint (make-design-graph (xt/db node #inst "2022-12-01T08") :r2))
  (uber/pprint (make-design-graph (xt/db node #inst "2022-12-13T08") :r2))
  (let [component-configuration-graph (make-design-graph node #inst "2022-11-14T08")] ;; still has a lot of choice
    )

  
  )


(xt/entity-history (xt/db node) :r2.electronics.power :desc {:with-docs? true})
;;  Now... list which ids have children that are NOT listed. 
(seq (xt/q (xt/db node) graph-ids)) ;; edges...
(xt/q (xt/db node) '{:find [e]
                     :where [[e :xt/id]
                             [e :component :r2]]})

(def g (create-graph-of (xt/db node) {:comp-param [:component :r2]}))

#_(uber/viz-graph g {:filename  (str "./output/graphs/g_at_" tt ".svg")})
#_(println (xt/entity-history (xt/db node) :timemeout :desc {:with-docs? true :with-corrections? true}))

;; a single DEVICE is a graph which is a union of the components, firmware, and location


(comment

  (->> (reduce into []
               (xt/q (xt/db node) '{:find [e]
                                    :where [[e :xt/id]
                                            [e :component :r2]]}))
       (mapv (fn [k] (xt/entity (xt/db node) k)))
       (remove #(= :disbanded (:current-state %)))
       (mapv (fn [m] (dissoc m :children :leaf :excludes))))
  (uber/pprint g)
  (xt/entity-history (xt/db node) :r2.electromechanical-assembly :desc {:with-docs? true})
  )

;; (hyperfiddle.rcf/enable!)
(tests
  ;; Show history of graph... 
 (render-design-graph-history node :r2)
 ;; https://graphviz.org/docs/outputs/imap/
 (get-edit-points-from-graph (xt/db node) g))

;; building a component. 
;; really, its a strict CONNECTED(?proof?) subgraph of the configuration. nothing more...


(uber/pprint g)
