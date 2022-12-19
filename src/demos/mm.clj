(ns demos.mm)


(defmulti encounter (fn [x y] [(:Species x) (:Species y)]))
(defmethod encounter [:Bunny :Lion] [b l] :run-away)
(defmethod encounter [:Lion :Bunny] [l b] :eat)
(defmethod encounter [:Lion :Lion] [l1 l2] :fight)
(defmethod encounter [:Bunny :Bunny] [b1 b2] :mate)
(def b1 {:Species :Bunny :other :stuff})
(def b2 {:Species :Bunny :other :stuff})
(def l1 {:Species :Lion :other :stuff})
(def l2 {:Species :Lion :other :stuff})
(encounter b1 b2)
;; -> :mate
(encounter b1 l1)
;; -> :run-away
(encounter l1 b1)
;; -> :eat
(encounter l1 l2)


"You can define hierarchical relationships with (derive child parent). 
 Child and parent can be either symbols or keywords, and must be namespace-qualified"
