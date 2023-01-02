(ns eplme.rendering.graphs
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [ubergraph.core :as uber]
            [com.brunobonacci.mulog :as u]
            [eplme.backend.loggo :refer [mulog]]
            [taoensso.tufte :as tufte :refer [defnp p profiled profile]]
            [riddley.walk :as r]))

(def ^:dynamic *render-options* {:folder "./resources/public/renders/"})

(defn- insert-name [s name]
  (let [[line rest] (str/split s #"\n" 2)]
    (str/join "\n" [(str "digraph " name " {") rest])))

(defn save 
  "need to add the name attribute, but the libraries have no clear way of doing it.
   So we modify the output file instead"
  ([g filename] (save g filename "TEST"))
  ([g filename name]
   (let [_ (uber/viz-graph g {:layout :dot :save {:filename filename :format :dot}})
         tmp-res (slurp filename)
         result (insert-name tmp-res name)]
     (spit filename result))))

(defnp render* [g opts]
  (let [{:keys [name folder]} opts
        ofolder (str folder name "/") 
        gname (str name ".g")
        picname (str name ".png")
        mapname (str name ".cmap")
        ]
    (u/with-context
      {:output-folder ofolder
       :graph g
       :name name}
      (u/trace ::render-process
               (do
                 (.mkdir (io/file folder))
                 (save g (str ofolder gname) name)
                 (u/log ::saved-render :v :v)
                 (let [r (sh/sh "dot"
                                "-Gdpi=300"
                                "-Timap" "-og.map"
                                "-Tpng" "-og.png"
                                "-Tcmap" "-og.cmap"
                                "-Tcmapx_np" "-og.cmapnp"
                                gname
                                :dir ofolder)]
                   (u/log ::converted-render :result r))))
      )))

(defn load-hashes [opts]
  (let [{:keys [folder]} opts]
    (into {} (->> (file-seq (io/file folder))
              (filter (fn [f] (= "hash.txt" (.getName f))))
              (map (juxt slurp #(.getParent %)))
              (map (fn [[hash folder]] [(Integer/parseInt hash) folder]))))))

(def hashes (atom (load-hashes *render-options*)))

(defn add-if-none [m k v]
  (if-not (contains? m k)
    (assoc m k v)
    m))


(defn format-graph-newtab [g]
  (reduce (fn [g nd]
            (uber/add-attr g nd :target "_blank")) g (uber/nodes g)))

(defnp render
  "Memoized rendering of graphs, using a custom hashing function for ubergraph graphs, as the :id field of the edges is 
   made with uuids, making the creation of graphs non-idempotent unless the random numbers are ignored"
  ([g]
   (render g
           (add-if-none *render-options* :name "TEST")
           (atom (empty @hashes))))
  ([g opts]
   (render g opts (atom (empty @hashes))))
  ([g opts cached-hashes]
   (let [{:keys [folder name]} opts
         h (hash [name
                  (set (->> (uber/edges g)
                            (map #(dissoc % :id))))
                  (set (let [g-nodes (uber/nodes g)]
                         (map (partial uber/node-with-attrs g) g-nodes)))])
         ofolder (str folder name "/")
         _ (io/make-parents (str ofolder "tmp"))]
     (if (contains? @cached-hashes h)
       (println "already rendered")
       (do
         (println "rendering...")
         (render* (format-graph-newtab g) opts)
         (swap! cached-hashes assoc h ofolder) 
         (spit (str ofolder "hash.txt") h))))))



(comment
  
  (def g (-> (uber/graph)
             (uber/add-nodes-with-attrs [:a {:URL "hi" }]
                                        [:b {:URL "dood"  }]
                                        [:c {:URL "'sup h" }])
             (uber/add-directed-edges [:a :b]
                                      [:c :a]
                                      [:c :b])))
  
  (render g) 
  (mulog)
  (require '[mount.core :as mount])
  (mount/stop)
  (mount/start)
  (render g (assoc *render-options* :name "TEST") hashes)
  

  "
dot -Timap -og.map -Tpng -og.png -Tcmap -og.cmap g.dot
 
"
  (sh/sh "dot" "-V")
  ;; this is how to render the proper 
  (sh/sh "dot" 
         "-Gdpi=300"
         "-Timap" "-og.map"
         "-Tpng" "-og.png"
         "-Tcmap" "-og.cmap"
         "-Tcmapx_np" "-og.cmapnp"
         "TEST.g"
         :dir "./resources/public/renders/TEST/") 
  )