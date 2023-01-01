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

(defn diff1d-m 
  "returns a stateful Diff function,
   (fn [result key new-item] ...)
   user can provide its own diff function
   which is called on each item (diff-fn item prev-item)
   The previous state is in a map, accessed by the key.
   
   Useful where doing a diff inside a reduce
   "
  ([]
   (diff1d-m -))
  ([diff-fn]
   (let [prev (atom {})]
     (fn differ [r k item]
       (let [prev-item (get @prev k)]
         (swap! prev assoc k item)
         (if (nil? prev-item)
           r
           (conj r (diff-fn item prev-item))))))))