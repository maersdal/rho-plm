(ns ptools.helpers
  "
  taken from:
  https://github.com/aphyr/dom-top/
  core.clj
  commit c6a9fdd jan 14 2022
  epl-1.0 license

   "
  (:require [riddley.walk :refer [macroexpand-all]]))


(defn rewrite-tails*
  "Helper for rewrite-tails which doesn't macroexpand."
  [f form]
  (if-not (seq? form)
    (f form)
    (case (first form)
      (do let* letfn*)
      (list* (concat (butlast form) [(rewrite-tails* f (last form))]))

      if
      (let [[_ test t-branch f-branch] form]
        (list 'if test (rewrite-tails* f t-branch) (rewrite-tails* f f-branch)))

      case*
      (let [[a b c d default clauses & more] form]
        (list* a b c d (rewrite-tails* f default)
               (->> clauses
                    (map (fn [[index [test expr]]]
                           [index [test (rewrite-tails* f expr)]]))
                    (into (sorted-map)))
               more))

      (f form))))

(defn rewrite-tails
  "Takes a Clojure form and invokes f on each of its tail forms--the final
  expression in a do or let, both branches of an if, values of a case, etc."
  [f form]
  (rewrite-tails* f (macroexpand-all form)))
