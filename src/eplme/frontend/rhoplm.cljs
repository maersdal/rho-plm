(ns eplme.frontend.rhoplm
  (:require
   [clojure.walk :refer [postwalk]]
   [goog.dom :as gdom]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [clojure.walk :as walk]
   [com.rpl.specter :as s :refer-macros [select transform]]))
;;https://github.com/reagent-project/reagent/blob/master/doc/InteropWithReact.md

(println "This text is printed from src/eplme/rhoplm.cljs. Go ahead and edit it and see reloading in action.")
(defn init []
  (println "init...."))
(defn multiply [a b] (* a b))

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Hello world!"}))

(defn get-app-element []
  (gdom/getElement "app"))

(def items {:root {:index :root
                   :isFolder true?
                   :children [:child1 :child2]
                   :data "Root item"}
            :child1 {:index :child1
                     :children []
                     :data "Child item 1"}
            :child2 {:index :child2
                     :children [:child3]
                     :data "Child item 2"}
            :child3 {:index :child3
                     :children []
                     :data "Child item 3"}})


#_(defn tree-fmt [items]
  [:ul (tree-seq (fn branch? [m]
                   (< 0 (count (get m :children []))))
                 (fn children [node]
                   (filter
                    (fn [item])
                    items)) 
                 )])
 #_(defn tree-fmt [items node]
   (let [
         _ (println (:data node) has-children?)]
     (if has-children?
       (let [children (map #(% items) (:children node))]
         (mapcat (partial tree-fmt items) children))
       #_(let [newvec (vec (cons :ul (mapv (fn [x]
                                           (tree-fmt items x)
                                           #_[:li (:data x)]) (->> (:children node)
                                                                   (map #(% items))))))]
         [:ul [:li (:data node)
               newvec]])
       #_[:ul (vec (concat [:li (:data node)]
                           (vec (cons :ul (mapv (fn [child-id] (tree-fmt items (child-id items))) (:children node))))))]

       #_(vec  (concat [:ul [:li (:data node)]]
                       (mapv (fn [child-id] (tree-fmt items (child-id items))) (:children node))))
       #_[:ul [:li (:data node)
               :ul (vec (map (fn [child-id] (tree-fmt items (child-id items))) (:children node)))]]
       #_[:ul (vec (concat [:li (:data node)] (map (fn [child-id] (tree-fmt items (child-id items))) (:children node))))]
       [:li (:data node)]))) 

;; [:a [:b :c] :d] < this kind of structure by recursion?
(def tinytree {:a {:index :a
                   :children [:b :c]}
               :b {:index :b
                   :children []}
               :c {:index :c
                   :children []}
               :d {:index :d
                   :children []}})
;; -> 
;; [:a [:b :c] :d] 
(def start-node :a)
#_(s/transform [s/MAP-VALS :children]
             (fn [x] (println x)
               identity)
             tinytree)
(defn has-children? [m]
  (< 0 (count (:children m))))
(->> tinytree
     (s/transform [s/MAP-VALS :index]
                  (fn [x]
                    [:ul [:li x]]))
     (s/transform [s/MAP-VALS has-children?]
                  (fn [x] 
                    (println "qq:" x)
                    #_(s/transform ) ;; [:ul [:li :c [:ul [:li a] [:li b]]]]
                    x)))
#_(defn get-children [tinytree start-node]
  (map (fn [k] (k tinytree)) (:children (start-node tinytree))))

;; => ([:a ({:index :b, :children []} {:index :c, :children []})] [:b ()] [:c ()] [:d ()])

;; => ([:a ({:index :b, :children []} {:index :c, :children []})] [:b ()] [:c ()] [:d ()]
#_(map (fn [kid] (tree-fmt items (kid items))) (:children (:root items)))
;; => ([:li "Child item 1"] [:ul [:li "Child item 2"] [:li "Child item 3"]])
;; (tree-fmt items (:root items))
;; => (:li "Child item 1" :li "Child item 3")
;; => [:ul [:li "Root item" [:ul [:li "Child item 1"] [:ul [:li "Child item 2" [:ul [:li "Child item 3"]]]]]]]
;; => [:ul [:li "Root item" [:ul [:li "Child item 1"] [:li "Child item 2"]]]]
;; => [:ul [:li "Root item" [:ul [[:li "Child item 1"] [:li "Child item 2"]]]]]
;; => 

 
;; REFERENCE 
 [:ul
  [:li (:data (:root items))
   [:ul
    [:li (:data (:child1 items))]
    [:ul [:li (:data (:child3 items))]]
    [:li (:data (:child2 items))]]]]

(filter #{:a } {:a 1 :b 2})
(cons :ul [:a :b])


(defn hello-world []
  [:div
   [:h1 (:text @app-state)]
   [:h3 "Edit this in src/eplme/rhoplm.cljs and watch it change!"]
   [:ul [:li "ass"]
    [:li "bitch"]]
   [:h3 "auto"]
  ;;  (tree-fmt items (:root items))
   [:h3 "manual"]
   [:ul [:li (:data (:root items))
         [:ul
          [:li (:data (:child1 items))]
          [:ul [:li (:data (:child3 items))]]
          [:li (:data (:child2 items))]]]]])

(comment 
  
  )

(defn mount [el]
  (rdom/render [hello-world] el))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
