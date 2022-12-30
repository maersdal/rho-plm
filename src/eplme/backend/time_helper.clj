(ns eplme.backend.time-helper
  (:require [java-time.api :as jt]
            [defun.core :refer [defun]]))

(defmulti dt "Returns delta time in milliseconds"
  (fn [a b]
    [(class a) (class b)]))

(defmethod dt [java.util.Date java.util.Date]
 [a b]
 (abs (- (.toEpochMilli (jt/instant a))
         (.toEpochMilli (jt/instant b)))))

(defmethod dt [java.lang.Long java.util.Date]
  [a b]
  (abs (- a
          (.toEpochMilli (jt/instant b)))))