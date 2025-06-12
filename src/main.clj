(ns main
  (:require [instaparse.core :as insta]
            [clojure.edn :as edn]))

(def ope
  (insta/parser
    "S = A|M|D|Dp|Ap|Mp|Q
    <Ap> = <'('> A <')'>
    A = (D|Dp|M|Mp|Ap) <W*> (<'+'> <W*> (D|Dp|M|Mp|Ap))+
    M = (D|Dp|Mp|Ap) <W*> (<'*'> <W*> (D|Dp|Ap|Mp))+
    <Mp> = <'('> M <')'>
    <Dp> = <'('> D <')'>
    D = #'\\d+'
    W = #' '
    Q = 'quit!'|'q!'
    "))

(defn opeeval
  [txt]
  (let [tree (ope txt)]
    (insta/transform {:D (fn [x] (edn/read-string x))
                      :A (fn [x & r] (apply + x r))
                      :M (fn [x & r] (apply * x r))
                      :S (fn [x] x)
                      :Q (fn [x] :stop)}
                     tree)))

(defn run
  [opts]
  (print "\nWelcome to proglang!\nThe programming language made for fun and learning :-)
         \n\nFor now, only the + and * operations exist.\nHave fun!\n\n")
  (loop [stop false]
    (if stop
      :stop
      (let [_ (print "> ")
            _ (flush)
            inp (read-line)
            outp (opeeval inp)
            mss (if (= outp :stop)
                  "\nBye!!\n"
                  (str " " outp "\n"))]
        (print mss)
        (recur (= outp :stop))))))

(defn -main
  [& args]
  (println "Hello from main!"))
