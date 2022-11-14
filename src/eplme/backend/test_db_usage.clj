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
           :notes ["built a prototype with a NMOS low-side switch"]}
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
           :notes ["Existing firmware must be repurposed"]}
          #inst "2022-10-15"]
         [{:xt/id :wifi-feather}
          #inst "2022-10-15"]
         [{:xt/id :computer-feather}
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
(def demo-firmware)


(xt/submit-tx node demo-components)
(xt/sync node)
(xt/entity-history (xt/db node) :r2.electronics.power :desc {:with-docs? true})
;;  Now... list which ids have children that are NOT listed. 
(require '[ubergraph.core :as uber])
(require '[ubergraph.alg :as uber-alg])
(seq (xt/q (xt/db node) graph-ids)) ;; edges...

(def g (let [nodes (reduce concat (xt/q (xt/db node) '{:find [e]
                                                        :where [[e :xt/id]]}))
             edges (seq (xt/q (xt/db node) graph-ids))
             nodes-with-inferred (seq (set (flatten edges)))
             filtered-edges (filter (fn [[a b]]
                                      (and (contains? (set nodes) a)
                                           (contains? (set nodes) b)))
                                    edges)]
         (-> (uber/graph)
             (uber/add-nodes* nodes-with-inferred)
             (uber/add-directed-edges* filtered-edges))))
(uber/pprint g)
#_(uber/viz-graph g)
(uber-alg/connected? g)
(uber-alg/dag? g)
(uber-alg/loners g)

