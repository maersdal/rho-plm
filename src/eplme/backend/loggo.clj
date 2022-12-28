(ns eplme.backend.loggo
  (:require [com.brunobonacci.mulog :as u]
            [mount.core :as mount]
            [clojure.java.io :as io]))


(mount/defstate mulog
  :start (do 
           (u/set-global-context! 
            {:app-name "rho-plm" 
             :version #_(slurp (io/resource "rho-plm.version")) "alpha"
             :env (or (System/getenv "ENV") "local")
             }) 
           (u/start-publisher! {:type :multi 
                                  :publishers
                                  [#_{:type :console}
                                   {:type :simple-file :filename "./output/logs/events.log"}
                                   {:type :zipkin
                                    :url  "http://localhost:9411/"
                                    :max-items     5000
                                    :publish-delay 5000}]}))
  :stop (mulog))


