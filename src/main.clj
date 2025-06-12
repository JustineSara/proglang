(ns main
  (:require [instaparse.core :as insta]
            [clojure.edn :as edn]))

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
    (insta/transform {:D (fn [x] (edn/read-string x))
                      :A (fn [x & r] (apply + x r))
                      :M (fn [x & r] (apply * x r))
                      :S (fn [x] x)}
                     tree)))

(defn run
  [opts]
  (print "\nWelcome to proglang!\nThe programming language made for fun and learning :-)
         \n\nFor now, only the + and * operations exist.\nHave fun!\n\n")
  (loop [stop false]
    (if stop
      (do (print "\nBye!!\n")
          :stop)
      (let [_ (print "> ")
            _ (flush)
            inp (read-line)]
        (if (= inp "q!")
          (recur true)
          (let [outp (opeeval inp)
                _ (print " " outp "\n")]
            (recur stop)))))))
