(ns main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [instaparse.core :as insta]))

(def ope
  (insta/parser
    "S = <nl>* sttmt (<nl>+ sttmt)* <nl>*
    <sttmt> = defn | exp | assign | Q
    defn = <'def '> <W*> Aname <W*> <'('> args <')'> <W*> <':'> <nl> flines
    args = arg? (<W*>','<W*> arg)* <W*>
    arg = name
    flines = (<'  '> (return | exp | assign) <nl>)+
    return = <'return '> <W*> exp
    assign = Aname <W*> <'='> <W*> exp
    Aname = name
    <exp> = A|M|D|Dp|Ap|Mp|Rname|fct
    fct = Rname <W*> <'('> <W*> exp? (<W*> <','> <W*> exp)* <W*> <')'>
    <Ap> = <'('> A <')'>
    <Mp> = <'('> M <')'>
    <Dp> = <'('> D <')'>
    <namep> = <'('> name <')'>
    A = (elem|M|Mp|Ap) <W*> (<'+'> <W*> (elem|M|Mp|Ap))+
    M = (elem|Mp|Ap) <W*> (<'*'> <W*> (elem|Ap|Mp))+
    D = #'\\d+'
    <elem> = D | Dp | Rname
    Rname = name | namep
    W = #' '
    Q = 'quit!'|'q!'
    keywords = 'return' | 'def'
    <name> = !keywords #'[a-zA-Z]\\w*'
    <nl> = '\n'
    "))

(defn opeeval
  [txt m]
  (let [tree (ope txt)
        a (atom m)]
    (insta/transform {:D (fn D [x] (edn/read-string x))
                      :A (fn A [& r] (apply + r))
                      :M (fn M [& r] (apply * r))
                      :exp (fn exp [x] [:result x])
                      :S (fn S  [& r] (concat [:result] r)) ;; we keep all the thingy ...
                      ;; which means we are not handling statement, including q! at all.
                      :assign (fn assign [x y] (if (= (first x) :Aname)
                                                 (do
                                                   (swap! a assoc (second x) y)
                                                   [:mem (second x) y])
                                                 [:error :assign]))
                      :Rname (fn Rname [x] (get @a x))
                      :Q (fn Q  [x] [:stop])}
                     tree)))

(defn run
  []
  (print "\nWelcome to proglang!\nThe programming language made for fun and learning :-)
         \n\nFor now, only the + and * operations exist.\nHave fun!\n\n")
  (loop [stop false
         m {}]
    (if stop
      :stop
      (let [_ (print "> ")
            _ (flush)
            inp (read-line)
            outp (last (opeeval inp m))
            mss (case (first outp)
                  :stop "\nBye!!\n"
                  :result (str " " (last outp) "\n")
                  :mem (format "%s := %s" (second outp) (last outp))
                  :error (str "ERROR: " (second outp))
                  (str "ERROR :O"))]
        (print mss)
        (recur (= (first outp) :stop)
               (if (= (first outp) :mem)
                 (assoc m (second outp) (last outp))
                 m))))))

(defn run-file
  [f]
  (if (.exists (io/file f))
    (let [inp (slurp f)
          outp (opeeval inp {})
          outpl (last outp)]
      (println)
      (println outp)
      (println outpl)
      (when (= (first outp) :result)
        outpl))
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
