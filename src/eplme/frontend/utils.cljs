(ns eplme.frontend.utils
  (:require [cljs.core.async :refer [<!] :refer-macros [go]]
            [cljs.core.async :as async]
            [cljs-http.client :as http]))

(defn rate-limit [f p]
  (let [cache (atom {})
        prev-t (atom (js/Date.))
        uncached-fn (fn [args]
                      (apply f args))
        update-cache (fn [args]
                       (let [new-v (uncached-fn args)
                             _ (swap! cache assoc args new-v)]
                         new-v))]
    (fn [& args]
      (let [cachehit (get @cache args ::nohit)
            dt (- (js/Date.) @prev-t)]
        (if (= ::nohit cachehit)
          (do
            (reset! prev-t (js/Date.))
            (update-cache args))
          (if (> dt p)
            (do
              (reset! prev-t (js/Date.))
              (update-cache args))
            cachehit))))))

(defn get-request [url]
  (go (<! (http/get url))))
(defn assoc-response [a rchan k]
  (go (let [resp (<! rchan)]
        (when (:success resp)
          (swap! a assoc k (:body resp))))))
(defn reset-response [a rchan]
  (go (let [resp (<! rchan)]
        (when (:success resp)
          (reset! a (:body resp))))))
(defn create-response-atom [api]
  (let [a (atom nil)]
    (reset-response a (get-request api))
    a))