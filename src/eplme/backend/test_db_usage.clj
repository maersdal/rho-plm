(ns eplme.backend.test-db-usage
  (:require [eplme.backend.db-primitives :refer [graph-ids]]
            [mount.core :as mount]
            [taoensso.tufte :as tufte]
            [xtdb.api :as xt]
            [ubergraph.alg :as uber-alg]))

(tufte/add-basic-println-handler! {})

(mount/start)
(def node (xt/start-node {}))

(def demo-components
  (mapv (fn [x]
          (vec (cons ::xt/put (assoc-in x [0 :type] :r2.components))))
        [[{:xt/id :r2.electromechanical-assembly
           :children [:r2.electronics :r2.mechatronics]}
          #inst "2022-09-01"]
         [{:xt/id :r2.mechatronics
           :current-state :r&d
           :children [:r2.motors
                      :r2.liquid-change-detector]}
          #inst "2022-10-01"]
         [{:xt/id :r2.electronics
           :current-state :r&d
           :children [:r2.electronics.power
                      :r2.electronics.physical.control
                      :r2.electronics.cloud
                      :r2.electronics.bluetooth
                      :r2.electronics.indicator]}
          #inst "2022-10-01"]
         [{:xt/id :r2.electronics.physical.control
           :current-state :r&d
           :components [:STMICRO32_MCU]
           :children [:pc.hand-sensor
                      :r2.liquid-change-detector
                      :r2.motor-driver-circuit]}
          #inst "2022-10-07"]
         [{:xt/id :r2.electronics.power
           :current-state :r&d
           :children [:5vreg
                      :3v3reg]}
          #inst "2022-10-01"]
         [{:xt/id :r2.liquid-change-detector
           :current-state :r&d
           :notes ["Capacitor discharged by removing liquid container, sensed and driven by mcu"]}
          #inst "2022-10-15"]
         [{:xt/id :r2.motors
           :current-state :testing
           :name "Generic DC Motor"
           :specs {:max-voltage-V 6
                   :stall-current-A 2}}
          #inst "2022-10-01"]
         [{:xt/id :r2.motor-driver-circuit
           :current-state :r&d}
          #inst "2022-10-14"]
         [{:xt/id :r2.motor-driver-circuit
           :current-state :testing
           :notes ["built a prototype with a NMOS low-side switch"]
           :children [:r2.motors]}
          #inst "2022-11-14"]
         [{:xt/id :5vreg}
          #inst "2022-08-31"]
         [{:xt/id :3v3reg}
          #inst "2022-08-31"]
         [{:xt/id :r2.electronics.cloud
           :children [:nrf9160-sparkfun
                      :wifi-feather
                      :computer-feather]}
          #inst "2022-10-15"]
         [{:xt/id :nrf9160-sparkfun
           :leaf true
           :notes ["Existing firmware must be repurposed"]}
          #inst "2022-10-15"]
         [{:xt/id :wifi-feather
           :leaf true}
          #inst "2022-10-15"]
         [{:xt/id :computer-feather
           :leaf true}
          #inst "2022-10-15"]
         [{:xt/id :r2.electronics.indicator
           :status :r&d
           :nodes ["LED, blinks when things are happening"]}
          #inst "2022-11-13"]
         [{:xt/id :r2.electronics.bluetooth
           :status :r&d
           :nodes ["Selected nrf52840"]}
          #inst "2022-08-31"]
         [{:xt/id :r2.electronics.bluetooth
           :status :testing
           :nodes ["Basic architecture for data reduction is implemented"]}
          #inst "2022-11-13"]
         [{:xt/id :pc.hand-sensor}
          #inst "2022-11-13"]]))

;; GIT integration..uri
(def demo-firmware
  (mapv (fn [x]
          (vec (cons ::xt/put (assoc-in x [0 :type] :r2.firmware))))
        [[{:xt/id :r2.algo-prototype-platform
           :notes ["Unix C codebase for evaluating algorithms"]
           :features ["Demo of radix tree 1 second rate-filter for bluetooth"]}
          #inst "2022-11-14T14"]]))


(xt/submit-tx node (vec (concat demo-firmware demo-components)))
(xt/sync node)
(xt/entity-history (xt/db node) :r2.electronics.power :desc {:with-docs? true})
;;  Now... list which ids have children that are NOT listed. 
(require '[ubergraph.core :as uber])
(require '[ubergraph.alg :as uber-alg])
(seq (xt/q (xt/db node) graph-ids)) ;; edges...
(xt/q (xt/db node) '{:find [e]
                     :where [[e :xt/id]
                             [e :type :r2.components]]})
(def g (let [nodes (reduce concat (xt/q (xt/db node) '{:find [e]
                                                        :where [[e :xt/id]
                                                                [parent :type :r2.components]]}))
             edges (seq (xt/q (xt/db node) (update graph-ids :where (fn [x] (vec (conj x ['parent :type :r2.components]))))))
             nodes-with-inferred (seq (set (flatten edges)))
             filtered-edges (filter (fn [[a b]]
                                      (and (contains? (set nodes) a)
                                           (contains? (set nodes) b)))
                                    edges)]
         (-> (uber/graph)
             (uber/add-nodes* nodes-with-inferred)
             (uber/add-directed-edges* filtered-edges))))
(uber/pprint g)
#_(uber/viz-graph g {:filename  (str "./output/graphs/g_at_" tt ".svg")})
(uber-alg/connected? g)
(uber-alg/dag? g)
(uber-alg/loners g)




#_(println (xt/entity-history (xt/db node) :timemeout :desc {:with-docs? true :with-corrections? true}))


;; a single DEVICE is a graph which is a union of the components, firmware, and location
;;  

;; Show history of graph... 

(defn get-edit-points-from-graph
  "Given a xt db node `node` and a graph `g`, return the distinct timestamps from where any
   node in that graph has been edited"
  [node g]
  (->>
   (mapcat (partial xt/entity-history (xt/db node)) (uber/nodes g) (repeat :desc))
   (map ::xt/valid-time)
   distinct
   sort))

(def story-points 
  (get-edit-points-from-graph node g))

(defn make-component-graph
  ([node]
   (make-component-graph node nil))
  ([node time]
   (let [db (xt/db node time)
         nodes (reduce concat (xt/q db '{:find [e]
                                         :where [[e :xt/id]
                                                 [parent :type :r2.components]]}))
         edges (seq (xt/q db (update graph-ids :where (fn [x] (vec (conj x ['parent :type :r2.components]))))))
         nodes-with-inferred (seq (set (flatten edges)))
         filtered-edges (filter (fn [[a b]]
                                  (and (contains? (set nodes) a)
                                       (contains? (set nodes) b)))
                                edges)]
     (-> (uber/graph)
         (uber/add-nodes* nodes-with-inferred)
         (uber/add-directed-edges* filtered-edges)))))



(->>
 story-points
 (map-indexed (fn [idx t] {:n idx
                           :graph (make-component-graph node t)}))
 (map (fn [{:keys [graph n]}]
        (uber/viz-graph
         graph
         {:save {:format :png
                 :filename  (str "./output/graphs/g_" (format "%03d" n) ".png")}}))))