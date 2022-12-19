(ns ptools.reducer
  "
  taken from:
  https://github.com/aphyr/dom-top/
  core.clj
  commit c6a9fdd jan 14 2022
  epl-1.0 license

   "
  
  (:require [ptools.helpers :refer [rewrite-tails]]
            [ptools.typehelpers :refer [mutable-acc-type]]))

(defmacro reducer
  "Syntactic sugar for writing reducing/transducing functions with multiple
  accumulators. Much like `loopr`, this takes a binding vector of loop
  variables and their initial values, a single binding vector for an element of
  the collection, a body which calls (recur) with new values of the
  accumulators (or doesn't recur, for early return), and a final expression,
  which is evaluated with the accumulators and returned at the end of the
  reduction. Returns a function with 0, 1, and 2-arity forms suitable for use
  with `transduce`.
    (transduce identity
               (reducer [sum 0, count 0]
                        [x]
                        (recur (+ sum x) (inc count))
                        (/ sum count))
               [1 2 2])
    ; => 5/3
  This is logically equivalent to:
    (transduce identity
               (fn ([] [0 0])
                   ([[sum count]] (/ sum count))
                   ([[sum count] x]
                    [(+ sum x) (inc count)]))
               [1 2 2])
  For zero and one-accumulator forms, these are equivalent. However, `reducer`
  is faster for reducers with more than one accumulator. Its identity arity
  creates unsynchronized mutable accumulators (including primitive types, if
  you hint your accumulator variables), and the reduction arity mutates that
  state in-place to skip the need for vector creation & destructuring on each
  reduction step. This makes it about twice as fast as a plain old reducer fn.
  These functions also work out-of-the-box with Tesser, clojure.core.reducers,
  and other Clojure fold libraries.
  If you want to use a final expression with a `reduced` form *and* multiple
  accumulators, add an `:as foo` to your accumulator binding vector. This
  symbol will be available in the final expression, bound to a vector of
  accumulators if the reduction completes normally, or bound to whatever was
  returned early. Using `:as foo` signals that you intend to use early return
  and may not *have* accumulators any more--hence the accumulator bindings will
  not be available in the final expression.
    (transduce identity
               (reducer [sum 0, count 0 :as acc]
                        [x]
                        (if (= count 2)
                          [:early sum]
                          (recur (+ sum x) (inc count)))
                        [:final acc])
               [4 1 9 9 9])
    ; => [:early 5]"
  [accumulator-bindings element-bindings body & [final]]
  (assert (even? (count accumulator-bindings)))
  (assert (= 1 (count element-bindings)))
  (let [element-name (first element-bindings)
        [acc-bindings [_ acc-as-name]] (split-with (complement #{:as})
                                                   accumulator-bindings)
        acc-pairs    (partition 2 acc-bindings)
        acc-names    (mapv first acc-pairs)
        acc-inits    (mapv second acc-pairs)
        acc-count    (count acc-names)]
    (if (< acc-count 2)
      ; Construct a plain old reducer
      (let [acc-name (or (first acc-names) '_)]
        `(fn ~(symbol (str "reduce-" element-name))
           ([] ~(first acc-inits))
           ([~acc-name] ~(if final final acc-name))
           ([~acc-name ~element-name]
            ~(rewrite-tails
              (fn rewrite-tail [form]
                (if (and (seq? form) (= 'recur (first form)))
                   ; We have a 0 or 1-arity recur form
                  (let [[_ acc-value] form]
                    (assert (= acc-count (count (rest form))))
                    acc-value)
                   ; Early return
                  `(reduced ~form)))
              body))))
      ; What kind of types does our accumulator need?
      (let [types    (map (comp :tag meta) acc-names)
            acc-type (symbol (.getName ^Class (mutable-acc-type types)))
            acc-name (with-meta (gensym "acc-") {:tag acc-type})
            fields   (->> (range (count types))
                          (map (comp symbol (partial str "x"))))
            ; [(. acc x0) (. acc x1) ...]
            get-fields (mapv (fn [type field]
                               (with-meta (list '. acc-name field)
                                 {:tag type}))
                             types fields)
            ; [foo (. acc x0) bar (. acc x1) ...]
            ; When we bind we want to strip hints off locals; compiler will
            ; complain
            bind-fields (vec (interleave
                              (map #(vary-meta % dissoc :tag) acc-names)
                              get-fields))
            ; The argument passed to our final arity
            final-name (gensym "final-")]
        `(fn ~(symbol (str "reduce-" element-name))
           ; Construct accumulator and initialize its fields
           ([] (let [~acc-name (new ~acc-type)
                     ; Bindings like [foo init, _ (set! (. acc x0) foo), ...]
                     ~@(mapcat (fn [acc-name field init]
                                 ; Can't type hint locals with primitive inits
                                 (let [acc (vary-meta acc-name dissoc :tag)]
                                   [acc init
                                    '_ (list 'set! field acc)]))
                               acc-names get-fields acc-inits)]
                 ~acc-name))
           ; Finalizer; destructure and evaluate final, or just return accs as
           ; vector
           ~(if final
              (if acc-as-name
                `([~final-name]
                  ; Bind input to acc-as-name, converting our mutable accs to a
                  ; vector.
                  (if (instance? ~acc-type ~final-name)
                    ; Normal return
                    (let [~acc-name ~final-name
                          ~acc-as-name ~get-fields]
                      ~final)
                    ; Early return
                    (let [~acc-as-name ~final-name]
                      ~final)))
                ; No early return. Just bind accs.
                `([~acc-name]
                  (let [~@bind-fields]
                    ~final)))
              ; No final expression; return reduced or a vector
              `([~final-name]
                (if (instance? ~acc-type ~final-name)
                  ; Normal return
                  (let [~acc-name ~final-name]
                    ~get-fields)
                  ; Early return
                  ~final-name)))
           ; Reduce: destructure acc and turn recur into mutations
           ([~acc-name ~element-name]
            (let ~bind-fields
              ~(rewrite-tails
                (fn rewrite-tail [form]
                  (if (and (seq? form) (= 'recur (first form)))
                     ; Recur becomes mutate and return acc
                    (do (assert (= acc-count (count (rest form))))
                        `(do ~@(map (fn [get-field value]
                                      `(set! ~get-field ~value))
                                    get-fields
                                    (rest form))
                             ~acc-name))
                     ; Early return becomes a reduced value
                    `(reduced ~form)))
                body))))))))