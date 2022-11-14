(ns eplme.backend.db-primitives
  (:require [clojure.pprint :as pprint]
            [eplme.backend.db-config :refer [xtdb-settings]]
            [malli.core :as m]
            [malli.util :as mu]
            [mount.core :as mount :refer [defstate]]
            [xtdb.api :as xt] 
            [taoensso.tufte :as tufte :refer  [defnp profiled profile]]
            [ubergraph.core :as uber]))


(defstate db
  :start (xt/start-node xtdb-settings)
  :stop (.close db))

;; rho-plm 
"
 each service is a graph of components.
 the components can change independently
 
 needs cycle detection just in case
"
(def child-rule
  '[(child-of [p] c)
    [p :children c]])
(def granddchild-rule
  '[(child-of [p] c)
    [p :children c1]
    (child-of c1 c)])

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



(comment
  (tufte/add-basic-println-handler! {})
  (require '[malli.generator :as mg])
  (mg/generate DagDocumentSchema)
  (m/validate DagDocumentSchema {:xt/id "ass"})
  (m/explain DagDocumentSchema {:xt/id "ass"})
  (m/explain DagDocumentSchema {:xt/id "ass"
                                :children [111]})
  db

  (mount/start)
  (def node (xt/start-node {}))
  (put-DAG-document node {:xt/id "ass"
                          :children [111]})
  (def manifest
    {:xt/id :manifest
     :pilot-name "Johanna"
     :id/rocket "SB002-sol"
     :id/employee "22910x2"
     :badges "SETUP"
     :cargo ["stereo" "gold fish" "slippers" "secret note"]})
  (def g0-3
    [{:xt/id :g0
      :children [:g1 :g2]}
     {:xt/id :g1
      :children [:g2 :g3]}
     {:xt/id :g2
      :children []}
     {:xt/id :g3
      :children [:g4]}
     {:xt/id :g4
      :children [:g5]}
     {:xt/id :g5
      :children []}])
  (xt/q (xt/db node)
        '{:find [e]
          :in [[entities ...]]
          :where [[e :xt/id entities]]}
        #{:qq :g0 :g1})
  (xt/q (xt/db node)
        '{:find [e]
          :in [[entities ...]]
          :where [[e :xt/id entities]]}
        [:qq :g0 :g1])
  (xt/submit-tx node (mapv (fn [x] [::xt/put x]) g0-3))
  (xt/entity (xt/db node) :manifest)
  (xt/entity (xt/db node) :g0)
  (xt/q (xt/db node) graph-ids-with-grandchildren)
  (put-DAG-document node {:xt/id :g5 :children [:g1]})
  (put-DAG-document node {:xt/id :g5 :children []})
  node
  )