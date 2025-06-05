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

(defn run
  [opts]
  (print "Hello World!"))
