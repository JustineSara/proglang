(ns main
  (:require [instaparse.core :as insta]))

(def ope
  (insta/parser
    "S = A|M|D|Ap|Mp
    <Ap> = <'('> A <')'>
    A = (D|M|Mp|Ap) <W*> (<'+'> <W*> (D|M|Mp|Ap))+
    M = (D|Mp|Ap) <W*> (<'*'> <W*> (D|Ap|Mp))+
    <Mp> = <'('> M <')'>
    D = #'\\d+'
    W = #' '
    "))

(defn run
  [opts]
  (print "Hello World!"))
