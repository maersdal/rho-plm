(ns eplme.frontend.rhoplm
  (:require [cljs-http.client :as http]
            [clojure.core.async :refer-macros [go] :refer [<!]]
            [eplme.frontend.utils :refer [rate-limit]]
            [goog.dom :as gdom] 
            [reagent.core :as reagent :refer [atom]]
            [reagent.dom :as rdom]))
;;https://github.com/reagent-project/reagent/blob/master/doc/InteropWithReact.md
(def class-api "/api/class/")
(def map-api "/api/demo/maps/")
(println "This text is printed from src/eplme/rhoplm.cljs. Go ahead and edit it and see reloading in action.")
(defn init []
  (println "init...."))
;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "ρ-PLM"
                          
                          :tutorial-step 0}))
(defn simple-getter [api]
  (let [options (atom nil)
        _ (go (reset! options (<! (http/get api))))]
    options))

(defn class-selector []
  (let [options (simple-getter class-api)]
    (fn []
      (into  [:select {:name "classtype"
                       :on-click #(do)
                       :on-change #(let [v (.. % -target -value)
                                         _ (prn v)]
                                     (http/put class-api {:edn-params {:chosen v}})
                                     (swap! app-state assoc :class-select v))}]
             (into [[:option {:value "all"} "Select none"]]
                   (mapv (fn [v]
                           [:option {:value v} v])
                     
                         (js->clj (:body @options))))))))


(comment
  [:div {:dangerouslySetInnerHTML {:__html code}}]
  (go (prn (:body (<! (http/get "/renders/TEST/g.map"))))) ;; works
  (http/put class-api {:form-params {:demo "vertitude"}})
  (def result-ref (atom ::new))
  (defn into-ref [chan]
    (go (reset! result-ref (<! chan))))
  (into-ref (http/get class-api))
  result-ref
  (let [a (atom nil)
        ch (http/get class-api)]
    (go (prn (<! ch))))
  (:class-select @app-state)
  (def ff (rate-limit (fn [q p] (prn "ASS!!" q p)) 2000))
  (ff "qq" "PPP")
  (def d0 (js/Date.))
  (def d1 (js/Date.))
  (- d1 d0)
  @app-state
  ;; (go (<! (http/get "api")))
  (def r (js/fetch "/api/greet/" (clj->js {:headers {:Content-type "application/edn"}
                                           :method "GET"})))



  (.-origin js/location)
  ;; => "http://localhost:8000"

  (clj->js {:headers {:Content-type "application/edn"}
            :method "GET"})
  ;; => #js {:headers #js {:Content-type "application/edn"}, :method "GET"}
  (js/alert "call on meee!")
  )

(defn call-button []
  (let [callcount (atom 0)]
    [:input {:type "Button" :value "HAI"
             :readOnly true
             :on-click (fn [_] 
                         (println (if (= 0 (mod (swap! callcount inc) 2))
                                    "CALL ME!"
                                    "CALL ON MEEEEE")))}]))

(defn map-disp [mapd]
  (fn [] 
    (let [map-loc (:cmap mapd)]
      (if (= nil map-loc)
        [:div "Waiting for data..."]
        (let [data (simple-getter map-loc)]
          (fn [map-loc]
            (let [b (js->clj (:body @data))]
              [:div {:style {"border" "1px solid red"
                             "scale" "0.5"}}
               [:p "Map disp"]
               [:img {:src (:png mapd) 
                      :useMap "#workmap"}]
               [:map {:name "workmap"
                      :dangerouslySetInnerHTML {:__html b}}]])))))))

(defn map-loader []
  (let [data (simple-getter map-api)]
    (fn []
      (let [b (js->clj (:body @data))]
        [:div [:p "Map-loader"] 
         [(map-disp b)] 
         ]))))
(comment 
  (slurp "/renders/TEST/g.map"))
#_(defn map-displayer [map]
  [map-loader])

(def tutorial-steps 
  [[:h3 "This is the tutorial"]
   [:h3 "It will show you how to display your systems"]
   [:div
    [:h3 "First:"]
    [:p "Select your class"]
    [:div [class-selector]]
    [:div [map-loader]]]])

(comment 
  ;; devs .. 
  (swap! app-state assoc :timer (js/Date.))
  (swap! app-state assoc :text "ρ-PLM")
  (swap! app-state assoc :tutorial-step 0)
  )
(defn get-app-element []
  (gdom/getElement "app"))

#_(defonce time-updater (js/setInterval
                       #(swap! app-state assoc :timer (js/Date.)) 1000))

(defn clock []
  (let [time (atom (js/Date.))
        time-updater (js/setInterval
                      #(reset! time (js/Date.)) 1000)]
    (fn []
      (let [time-str (str @time)]
        [:div.clock time-str]))))

(defn tutorial-nav []
  [:div 
   [:input {:type "button" :value "back" :on-click #(swap! app-state update :tutorial-step (fn [x] (mod (dec x) (count tutorial-steps))))}]
   "." (:tutorial-step @app-state) "."
   [:input {:type "button" :value "forward" :on-click #(swap! app-state update :tutorial-step (fn [x] (mod (inc x) (count tutorial-steps))))}]
   (get tutorial-steps (:tutorial-step @app-state))])

(defn hello-world []
  [:div.page
   [:header
    [:h1 (:text @app-state)]]
   [:div.sidebar 
    [:p "Sidebar thingy you know.. because we're cool like that."]
    [call-button]]
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
