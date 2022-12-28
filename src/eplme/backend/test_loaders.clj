(ns eplme.backend.test-loaders
  (:require [clojure.edn :as edn]
            [eplme.backend.common :refer [sha256]]
            [xtdb.api :as xt]))



(def demo-components
  (mapv (fn [x]
          (vec (cons ::xt/put (assoc-in x [0 :component] :r2))))
        (edn/read-string (slurp "src/demodata/demo_components.edn"))))

(def demo-firmware
  (mapv (fn [x]
          (vec (cons ::xt/put (assoc-in x [0 :firmware] :r2))))
        [[{:name :r2.demo-failure
           :commit-msg "first"
           :xt/id (sha256 "first")}
          #inst "2022-10-14T14"]
         [{:name :r2.demo-failure
           :commit-msg "second"
           :notes ["Failure introduced here for demonstration purposes"] ;; todo write example code on howto find this failure correlation
           :xt/id (sha256 "second")}
          #inst "2022-10-15T14"]
         [{:name :r2.demo-failure
           :commit-msg "third"
           :xt/id (sha256 "third")}
          #inst "2022-10-16T14"]
         [{:name :r2.test-fw
           :commit-msg "third"
           :xt/id (sha256 "test-fw")}
          #inst "2022-10-16T14"]]))

(def demo-units
  (mapv (fn [x]
          (vec (cons ::xt/put (assoc-in x [0 :units] :r2))))
        [[{:xt/id 1
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :site :resani
           :location :garage
           :firmware-sha (sha256 "third")}
          #inst "2022-10-16T15"]
         [{:xt/id 2
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :site :resani
           :location :garage
           :firmware-sha (sha256 "first")}
          #inst "2022-10-14T15"]
         [{:xt/id 2
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :site :magnus
           :location :house
           :firmware-sha (sha256 "second")}
          #inst "2022-10-15T15"]
         [{:xt/id 2
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :site :magnus
           :location :house
           :firmware-sha (sha256 "third")}
          #inst "2022-10-16T15"]
         [{:xt/id 3
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :site :magnus
           :location :house
           :firmware-sha (sha256 "nonexistent")}
          #inst "2022-10-16T15"]
         [{:xt/id 4
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :site :magnus
           :location :house
           :firmware-sha (sha256 "test-fw")}
          #inst "2022-10-16T15"]
         [{:xt/id 5
           :r.configuration :r.configs.2022.11.14
           :r.firmware :r2.demo-failure
           :site :magnus
           :location :house
           :firmware-sha (sha256 "first")}
          #inst "2022-10-16T15"]]))
