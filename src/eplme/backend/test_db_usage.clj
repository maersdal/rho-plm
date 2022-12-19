(ns eplme.backend.test-db-usage
  (:require [clojure.edn :as edn]
            [eplme.backend.component-graph :refer [create-graph-of
                                                   get-edit-points-from-graph
                                                   make-component-graph render-component-graph-history]]
            [eplme.backend.db-primitives :refer [find-excludes graph-ids]]
            [hyperfiddle.rcf :refer [tests]]
            [mount.core :as mount]
            [taoensso.tufte :as tufte]
            [ubergraph.core :as uber]
            [xtdb.api :as xt])
  (:import [java.security MessageDigest]))

(tufte/add-basic-println-handler! {})

(mount/start)
(def node (xt/start-node {}))

(def demo-components
  (mapv (fn [x]
          (vec (cons ::xt/put (assoc-in x [0 :component] :r2))))
        (edn/read-string (slurp "src/demodata/demo_components.edn"))))

(defn sha256 [string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))
;; GIT integration..uri
(def demo-firmware
  (mapv (fn [x]
          (vec (cons ::xt/put (assoc-in x [0 :firmware] :r2))))
        [[{:xt/id :r2.algo-prototype-platform
           :notes ["Unix C codebase for evaluating algorithms"]
           :features ["Demo of radix tree 1 second rate-filter for bluetooth"]}
          #inst "2022-11-14T14"]
         [{:xt/id :r2.algo-prototype-platform
           :notes ["Unix C codebase for evaluating algorithms"]
           :features ["Demo of radix tree 1 second rate-filter for bluetooth"]}
          #inst "2022-11-14T14"]
         [{:xt/id :r2.demo-failure
           :commit-msg "first"
           :sha (sha256 "first")}
          #inst "2022-10-14T14"]
         [{:xt/id :r2.demo-failure
           :commit-msg "second"
           :notes ["Failure introduced here for demonstration purposes"] ;; todo write example code on howto find this failure correlation
           :sha (sha256 "second")}
          #inst "2022-10-15T14"]
         [{:xt/id :r2.demo-failure
           :commit-msg "third"
           :sha (sha256 "third")}
          #inst "2022-10-16T14"]]))

(xt/submit-tx node (vec (concat demo-firmware demo-components)))
(xt/sync node)


(def demo-configurations
  (mapv (fn [x]
          (vec (cons ::xt/put (assoc-in x [0 :configurations] :r2))))
        [[{:xt/id :r.configs.2022.11.14
           :config-snapshot-time #inst "2022-11-14T08"
           :edges 'edges
           :nodes 'nodes}
          #inst "2022-11-14"]]))

(xt/entity (xt/db node) :nrf9160-sparkfun)
(xt/entity (xt/db node) :computer-feather)

(find-excludes (xt/db node) :nrf9160-sparkfun)
 
(defn create-configuration-from-graph [node g name]
  )

(comment
  (def db (xt/db node))

  (uber/pprint (make-component-graph (xt/db node #inst "2022-12-01T08") :r2))
  (uber/pprint (make-component-graph (xt/db node #inst "2022-12-13T08") :r2))
  (let [component-configuration-graph (make-component-graph node #inst "2022-11-14T08")] ;; still has a lot of choice
    )
  )

(def demo-units
  (mapv (fn [x]
          (vec (cons ::xt/put (assoc-in x [0 :units] :r2))))
        [[{:xt/id 1
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :firmware-sha (sha256 "third")}
          #inst "2022-10-16T15"]
         [{:xt/id 2
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :firmware-sha (sha256 "first")}
          #inst "2022-10-14T15"]
         [{:xt/id 2
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :firmware-sha (sha256 "second")}
          #inst "2022-10-15T15"]
         [{:xt/id 2 
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :firmware-sha (sha256 "third")}
          #inst "2022-10-16T15"]]))

(xt/submit-tx node (vec (concat demo-configurations demo-units)))
(xt/sync node)


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
;;  

(comment
  (xt/entity-history (xt/db node) :r2.electromechanical-assembly :desc {:with-docs? true})
  
  )

;; (hyperfiddle.rcf/enable!)
(tests
  ;; Show history of graph... 
 (render-component-graph-history node :r2)
 ;; https://graphviz.org/docs/outputs/imap/
 (get-edit-points-from-graph (xt/db node) g)
 
 )

;; building a component. 
;; really, its a strict CONNECTED(?proof?) subgraph of the configuration. nothing more...


(uber/pprint g)
