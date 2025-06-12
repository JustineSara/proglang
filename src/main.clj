(ns main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [instaparse.core :as insta]))

(def ope
  (insta/parser
    "S = (exp|Q) (<'\\n'> (exp|Q))*
    exp = A|M|D|Dp|Ap|Mp
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
    (insta/transform {:D (fn d [x] (edn/read-string x))
                      :A (fn a [& r] (apply + r))
                      :M (fn m [& r] (apply * r))
                      :exp (fn bla [x] [:result x])
                      :S (fn s  [& r] (last r)) ;; we keep the last thingy ...
                      ;; which will be a problem if q! is in the middle of a program (for now)
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
            outp (opeeval inp)
            mss (case (first outp)
                  :stop "\nBye!!\n"
                  :result (str " " (second outp) "\n")
                  (str "ERROR :O"))]
        (print mss)
        (recur (= (first outp) :stop))))))

(defn run-file
  [f opt]
  (if (.exists (io/file f))
    (let [alltxt (s/split-lines (slurp f))]
      (loop [a alltxt
             r nil]
        (if (empty? a)
          (do
            (println)
            (println r))
          (let [[inp & a] a
                outp (opeeval inp)]
            (when (= opt "-sai") (println "> " inp))
            (when (not (nil? opt)) (println "  " outp))
            (recur a outp)))))
    (println "/!\\ The file " f "does not exist. Check your file path and name!")))

(defn usage
  []
  (println "Usage: proglang [file] [options]")
  (println)
  (println "- if no arguments are provided, starts a shell")
  (println "- if file is provided, run the file and print the end result")
  (println "- options are only valid with files:")
  (println "\t * -sao : show all outputs. Print each result from each line.")
  (println "\t * -sai : show all inputs. Print each input line followed by its result."))

(defn -main
  [& args]
  (case (count args)
    0 (run)
    1 (let [f (first args)]
        (if (or (= f "-u") (= f "-h"))
          (usage)
          (run-file f nil)))
    2 (let [f (first args)
            opt (second args)]
        (if (or (= opt "-sao") (= opt "-sai"))
          (run-file f opt)
          (usage)))
    (usage)))
