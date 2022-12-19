(ns eplme.rendering.graphs
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ubergraph.core :as uber]
            [taoensso.tufte :as tufte :refer [defnp p profiled profile]]))

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
  (let [{:keys [name]} opts
        ofolder (str "./public/renders/" name "/") 
        gname (str name ".g")
        picname (str name ".png")
        mapname (str name ".cmap")
        ] 
    (.mkdir (io/file ofolder))
    (save g (str ofolder gname) name)))

(def hashes (atom (set (->> (file-seq (io/file "./public/renders/"))
                            (filter (fn [f] (= "hash.txt" (.getName f))))
                            (map slurp)
                            (map #(Integer/parseInt %))))))
(defnp render
  "Memoized rendering of graphs, using a custom hashing function for ubergraph graphs, as the :id field of the edges is 
   made with uuids, making the creation of graphs non-idempotent unless the random numbers are ignored"
  [g opts]
  (let [h (hash [(:name opts)
                 (set (->> (uber/edges g)
                           (map #(dissoc % :id))))
                 (set (let [g-nodes (uber/nodes g)]
                        (map (partial uber/node-with-attrs g) g-nodes)))])
        ofolder (str "./public/renders/" (:name opts) "/")]
    (if (contains? @hashes h)
      (println "already rendered")
      (do
        (println "rendering...")
        (render* g opts)
        (swap! hashes conj h)
        (spit (str ofolder "hash.txt") h)))))



(comment
  (def g (-> (uber/graph)
             (uber/add-nodes-with-attrs [:a {:URL "hi"}]
                                        [:b {:URL "dood"}]
                                        [:c {:URL "'sup h"}])
             (uber/add-directed-edges [:a :b]
                                      [:c :a]
                                      [:c :b])))
  (render g {:name "testg"})
  

  "
dot -Timap -og.map -Tpng -og.png -Tcmap -og.cmap g.dot
 
"
  )