(ns eplme.backend.fnschemas
  (:require [hyperfiddle.rcf :refer [tests]]
            [malli.core :as m]
            [malli.error :as me]
            [xtdb.api :as xt]))

(def xtdb-node 
  (m/schema [:fn {:error/fn
                  (fn [error _]
                    (str "should be a class xtdb.node.XtdbNode, was " (class (:value error))))} 
             (fn [node] (= xtdb.node.XtdbNode (class node)))]))
(def xtdb-db 
  (m/schema [:fn {:error/fn
                  (fn [error _]
                    (str "should be a class xtdb.query.QueryDatasource, was " (class (:value error))))} 
             (fn [db] (= xtdb.query.QueryDatasource (class db)))]))
(def valid-xt-db? 
  (m/validator xtdb-db))
(def valid-xt-node?
  (m/validator xtdb-node))

(defmacro assert->true
  "like assert but returns true" 
  ([x]
   (when *assert*
     `(if-not ~x
        (throw (new AssertionError (str "Assert failed: " (pr-str '~x))))
        true)))
  ([x message]
   (when *assert*
     `(if-not ~x
        (throw (new AssertionError (str "Assert failed: " ~message "\n" (pr-str '~x))))
        true))))

(defn check-schema
  ([validator schema x]
   (assert->true (validator x) (me/humanize (m/explain schema x))))
  ([schema x]
   (assert->true (m/validate schema x) (me/humanize (m/explain schema x)))))


(defn check-xt-db "Checks if thing is xt-db, throws with nice message if not, returns true"
  [x]
  (check-schema valid-xt-db? xtdb-db x))
(defn check-xt-node "Checks if thing is xt-nodem throws with nice message if not, returns true" [x]
  (check-schema valid-xt-node? xtdb-node x))

(comment
  (check-schema valid-xt-db? xtdb-db (xt/start-node {}))
  (require '[xtdb.api :as xt])
  (-> xtdb-db
      (m/explain (xt/start-node {}))
      (me/humanize))
  (check-xt-db (xt/db (xt/start-node {})))
  (check-xt-node (xt/start-node {}))
  (check-xt-node (xt/db (xt/start-node {})))
  
  )

(tests
 "Malli xt valid"
 (require '[xtdb.api :as xt])
 (m/validate xtdb-node (xt/start-node {})) := true
 (m/validate xtdb-node (xt/db (xt/start-node {}))) := false
)


;(hyperfiddle.rcf/enable!)

