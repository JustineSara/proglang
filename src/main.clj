(ns main
  (:require [instaparse.core :as insta]))

(def ope
  (insta/parser
    "S = A|M|D
    A = (D|M) <W*> (<'+'> <W*> (D|M))+
    M = D <W*> (<'*'> <W*> D)+
    D = #'\\d+'
    W = #' '
    "))

(defn run
  [opts]
  (print "Hello World!"))
