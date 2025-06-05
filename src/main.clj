(ns main
  (:require [instaparse.core :as insta]))

(def ope
  (insta/parser
    "S = A|M|D|Dp|Ap|Mp
    <Ap> = <'('> A <')'>
    A = (D|Dp|M|Mp|Ap) <W*> (<'+'> <W*> (D|Dp|M|Mp|Ap))+
    M = (D|Dp|Mp|Ap) <W*> (<'*'> <W*> (D|Dp|Ap|Mp))+
    <Mp> = <'('> M <')'>
    <Dp> = <'('> D <')'>
    D = #'\\d+'
    W = #' '
    "))

(defn opeeval
  [txt]
  (let [tree (ope txt)]
    (insta/transform {:D (fn [x] (parse-double x))
                      :A (fn [x & r] (apply + x r))
                      :M (fn [x & r] (apply * x r))
                      :S (fn [x] x)}
                     tree)))

(defn run
  [opts]
  (print "Hello World!"))
