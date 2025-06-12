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
            mss (if (= outp :stop)
                  "\nBye!!\n"
                  (str " " outp "\n"))]
        (print mss)
        (recur (= outp :stop))))))

(defn run-file
  [f opt]
  (println "Code does not exist yet, will do soon")
  (println "File:" f)
  (println "Option:" opt))

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
