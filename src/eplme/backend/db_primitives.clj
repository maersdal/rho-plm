(ns eplme.backend.db-primitives
  (:require [clojure.pprint :as pprint]
            [eplme.backend.db-config :refer [xtdb-settings]]
            [malli.core :as m]
            [malli.util :as mu]
            [mount.core :as mount :refer [defstate]]
            [taoensso.tufte :as tufte :refer  [defnp profile profiled]]
            [ubergraph.core :as uber]
            [user :refer [vars->map]]
            [xtdb.api :as xt]))


(defstate db
  :start (xt/start-node xtdb-settings)
  :stop (.close db))

;; rho-plm 
"
 each service is a graph of components.
 the components can change independently
 
 needs cycle detection just in case
"

(defn graph-query-builder
  ([] (graph-query-builder {}))
  ([{:keys [child-key]
     :or {child-key :children}}]
   {:child-rule ['(child-of [p] c)
                 ['p child-key 'c]]

    :granddchild-rule
    ['(child-of [p] c)
     ['p child-key 'c1]
     '(child-of c1 c)]}))

(def gqs (graph-query-builder))

(def child-rule
  (:child-rule gqs))

(def granddchild-rule
  (:granddchild-rule gqs))

(def graph-ids-with-grandchildren
  {:find '[parent child]
   :where '[[parent :xt/id]
            (child-of parent child)]
   :rules [child-rule
           granddchild-rule]})
(def graph-ids
  {:find '[parent child]
   :where '[[parent :xt/id]
            (child-of parent child)]
   :rules [child-rule]})
(def graph-cycles
  {:find '[parent]
   :where '[[parent :xt/id]
            (child-of parent child)
            [(= parent child)]]
   :rules [child-rule
           granddchild-rule]})

(def XtIdTypes
  [:orn
   [:keyword keyword?]
   [:text string?]
   [:uuid uuid?]])

(def DagDocumentSchema
  [:map
   [:xt/id XtIdTypes]
   [:children {:optional true} [:vector {:default []} XtIdTypes]]])

#_(defn DAG-ops-verified-operations
  "xt-ops in the form of [[::xt/put {document...} valid-t end-t]]"
  [node xt-ops]
  (let [docs (mapcat second xt-ops)
        _ (assert (every? true? (map (partial m/validate DagDocumentSchema) docs))
                  "All documents must conform to DAG schema")
        tdb (xt/with-tx (xt/db node) xt-ops)
        cycle (vec (flatten (seq (xt/q tdb graph-cycles))))]
    (if-not (seq cycle)
      (xt/submit-tx node xt-ops)
      (let [failure-text (str "error: operations " xt-ops
                              " would introduce cycles in the graph: "
                              (filterv (fn [[a b]]
                                         (or (= input-id a)
                                             (= input-id b)))
                                       (xt/q tdb graph-ids)))]
        (println failure-text)
        {:error failure-text}))))



(defnp put-DAG-document
  "Insert a document, first checking if the document will create a valid DAG.
   TODO children must exist in database?"
  [node input-doc opts] 
  {:pre [(if (m/validate DagDocumentSchema input-doc)
           true
           (do (println "ERROR, schema failure: ")
               (pprint/pprint (m/explain DagDocumentSchema input-doc))))]}
  (let [input-id (:xt/id input-doc)
        input-children (:children input-doc)
        tdb (xt/with-tx (xt/db node) [[::xt/put input-doc]])
        cycle (vec (flatten (seq (xt/q tdb graph-cycles))))]
    (if-not (seq cycle)
      (xt/submit-tx node [[::xt/put input-doc]])
      (let [failure-text (str "error: insertion of " (mapv (partial vector input-id) input-children)
                              " would introduce cycles between nodes of the tree: "
                              (filterv (fn [[a b]]
                                         (or (= input-id a)
                                             (= input-id b)))
                                       (xt/q tdb graph-ids)))]
        (println failure-text)
        {:error failure-text}))))


(defn find-excludes [db id]
  (disj (set (flatten (seq (xt/q db
                                 '{:find [e xs]
                                   :in [id]
                                   :where [[e :xt/id]
                                           [e :excludes xs]
                                           (or [(= xs id)]
                                               [(= e id)])]}
                                 id))))
        id))


