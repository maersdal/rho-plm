(ns user)

(defmacro vars->map 
  "Takes vars and returns a map of their names 
   such that (= (vars->map a b) {:a a :b b})"
  [& vars]
  (zipmap (map (comp keyword name) vars)
          vars))