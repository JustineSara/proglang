(ns hello-instaparse
  (:require [instaparse.core :as insta]))

(def as-and-bs
  (insta/parser
    "S = AB*
    AB = A B
    A = 'a'+
    B = 'b'+"))

(comment
  (as-and-bs "aaaabb"))
