(ns eplme.backend.common
  (:import [java.security MessageDigest]))

(defn sha256 [string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))

(defn ms->days [ms]
  (/ ms
     (* 1000.0 3600 24)))
(defn average [xs]
  (/ (reduce + xs)
     (count xs)))