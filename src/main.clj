(ns main
  (:require [instaparse.core :as insta]))

(def ope
  (insta/parser
    "S = A ('+' A)*
    A = D | (D ('*' D)*)
    D = #'\\d+'
    "))

(defn run
  [opts]
  (print "Hello World!"))
