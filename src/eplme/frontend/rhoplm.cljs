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
;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "ρ-PLM"
                          :timer (js/Date.)
                          :tutorial-step 0}))
(def tutorial-steps 
  [[:h3 "This is the tutorial"]
   [:h3 "It will show you how to display your systems"]])
(defonce time-updater (js/setInterval
                       #(swap! app-state assoc :timer (js/Date.)) 1000))
(comment 
  ;; devs .. 
  (swap! app-state assoc :timer (js/Date.))
  (swap! app-state assoc :text "ρ-PLM")
  (swap! app-state assoc :tutorial-step 0)
  )
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

(defn clock []
  (let [time-str (str (:timer @app-state))]
    [:div.clock time-str]))

(defn tutorial-nav []
  [:div 
   [:input {:type "button" :value "back" :on-click #(swap! app-state update :tutorial-step (fn [x] (max 0 (dec x))))}]
   "." (:tutorial-step @app-state) "."
   [:input {:type "button" :value "forward" :on-click #(swap! app-state update :tutorial-step (fn [x] (mod (inc x) (count tutorial-steps))))}]
   (get tutorial-steps (:tutorial-step @app-state))])

(defn hello-world []
  [:div.page
   [:header
    [:h1 (:text @app-state)]]
   [:div.sidebar 
    [:p "Sidebar thingy you know.. because we're cool like that."]]
   [:div.main 
    [clock]
    [:h2 "Fact-based graph representation of the lifetime of your system components"]
    [:div.tutorial
     [tutorial-nav]]]
   [:footer
    [:p "© 2023 Magnus Rentsch Ersdal"]]])


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
