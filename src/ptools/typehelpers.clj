(ns ptools.typehelpers
  "
  taken from:
  https://github.com/aphyr/dom-top/
  core.clj
  commit c6a9fdd jan 14 2022
  epl-1.0 license
"
  (:import
    ; oh no
   (clojure.asm ClassVisitor
                ClassWriter
                Opcodes
                Type)
   (clojure.lang DynamicClassLoader
                 RT)))


(defonce
  ^{:doc "A mutable cache of mutable accumulator types we've generated. Stores
         a map of type hints (e.g. ['long 'Object]) to classes (e.g.
         MutableAcc-long-Object)."}
  mutable-acc-cache*
  (atom {}))


(defn type->desc
  "Takes a type (e.g. 'int, 'objects, 'longs, 'Foo) and converts it to a JVM
  type descriptor like \"I\"."
  [t]
  (.getDescriptor
   (case t
     byte          Type/BYTE_TYPE
     short         Type/SHORT_TYPE
     int           Type/INT_TYPE
     long          Type/LONG_TYPE
     float         Type/FLOAT_TYPE
     double        Type/DOUBLE_TYPE
     bytes         (Type/getType "[B")
     shorts        (Type/getType "[S")
     ints          (Type/getType "[I")
     longs         (Type/getType "[J")
     floats        (Type/getType "[F")
     doubles       (Type/getType "[D")
     objects       (Type/getType "[Ljava/lang/Object;")
      ; Everything else is an Object for us
     (Type/getType Object))))

(defn mutable-acc-type
  "Takes a list of types as symbols and returns the class of a mutable
  accumulator which can store those types. May compile new classes on the fly,
  or re-use a cached class.
  This method largely courtesy of Justin Conklin! *hat tip*"
  [types]
  (let [; All objects are the same as far as we're concerned.
        types (mapv (fn [type]
                      (if (and type (re-find #"^[a-z]" (name type)))
                        type
                        'Object))
                    types)]
    (or (get @mutable-acc-cache* types)
        (let [class-name (str "dom-top.core.MutableAcc-"
                              (str/join "-" (map name types)))
              base-type "java/lang/Object"
              ; Construct class bytecode
              cv (doto (ClassWriter. ClassWriter/COMPUTE_FRAMES)
                   (.visit Opcodes/V1_7
                           (bit-or Opcodes/ACC_PUBLIC Opcodes/ACC_FINAL)
                           (.replace class-name \. \/)
                           nil base-type nil))]
          ; Constructor
          (doto (.visitMethod cv Opcodes/ACC_PUBLIC "<init>" "()V" nil nil)
            (.visitCode)
            (.visitVarInsn Opcodes/ALOAD 0)
            ; Super
            (.visitMethodInsn Opcodes/INVOKESPECIAL base-type
                              "<init>" "()V" false)
            (.visitInsn Opcodes/RETURN)
            (.visitMaxs -1 -1)
            (.visitEnd))
          ; Fields
          (doseq [[i t] (map vector (range) types)]
            (doto (.visitField cv Opcodes/ACC_PUBLIC (str "x" i)
                               (type->desc t) nil nil)
              (.visitEnd)))
          ; And load
          (let [bytes ^bytes (.toByteArray cv)
                loader ^clojure.lang.DynamicClassLoader (RT/makeClassLoader)
                klass (.defineClass loader class-name bytes nil)]
            (swap! mutable-acc-cache* assoc types klass)
            klass)))))