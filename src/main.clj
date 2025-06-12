(ns main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [instaparse.core :as insta]))

(def ope
  (insta/parser
    "S = (exp|Q) (<'\\n'> (exp|Q))* <lastline>?
    exp = A|M|D|Dp|Ap|Mp
    <Ap> = <'('> A <')'>
    A = (D|Dp|M|Mp|Ap) <W*> (<'+'> <W*> (D|Dp|M|Mp|Ap))+
    M = (D|Dp|Mp|Ap) <W*> (<'*'> <W*> (D|Dp|Ap|Mp))+
    <Mp> = <'('> M <')'>
    <Dp> = <'('> D <')'>
    D = #'\\d+'
    W = #' '
    Q = 'quit!'|'q!'
    lastline = '\\n'
    "))

(defn opeeval
  [txt]
  (let [tree (ope txt)]
    (insta/transform {:D (fn d [x] (edn/read-string x))
                      :A (fn a [& r] (apply + r))
                      :M (fn m [& r] (apply * r))
                      :exp (fn bla [x] [:result x])
                      :S (fn s  [& r] r) ;; we keep all the thingy ...
                      ;; which means we are not handling statement, including q! at all.
                      :Q (fn q  [x] [:stop])}
                     tree)))

(defn run
  []
  (print "\nWelcome to proglang!\nThe programming language made for fun and learning :-)
         \n\nFor now, only the + and * operations exist.\nHave fun!\n\n")
  (loop [stop false]
    (if stop
      :stop
      (let [_ (print "> ")
            _ (flush)
            inp (read-line)
            outp (last (opeeval inp))
            mss (case (first outp)
                  :stop "\nBye!!\n"
                  :result (str " " (second outp) "\n")
                  (str "ERROR :O"))]
        (print mss)
        (recur (= (first outp) :stop))))))

(defn run-file
  [f]
  (if (.exists (io/file f))
    (let [inp (slurp f)
          outp (opeeval inp)]
      (println)
      (println outp)
      (println (last outp)))
    (println "/!\\ The file " f "does not exist. Check your file path and name!")))

(defn usage
  []
  (println "Usage: proglang [file]")
  (println)
  (println "- if no arguments are provided, starts a shell")
  (println "- if file is provided, run the file and print the end result"))

(defn -main
  [& args]
  (case (count args)
    0 (run)
    1 (let [f (first args)]
        (if (or (= f "-u") (= f "-h"))
          (usage)
          (run-file f)))
    (usage)))
