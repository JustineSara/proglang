(ns main
  (:require [instaparse.core :as insta]))

(def ope
  (insta/parser
    "S = A <W*> ('+' <W*> A)*
    A = D <W*> ('*' <W*> D)*
    D = #'\\d+'
    W = #' '
    "))

(defn run
  [opts]
  (print "Hello World!"))
