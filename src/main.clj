(ns main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [instaparse.core :as insta]))

(def ope
  (insta/parser
    "S = <nl>* sttmt (<nl>+ sttmt)* <nl>*
    <sttmt> = if | defn | exp | assign | Q
    if = <'if '> <W*> exp <W*> <':'> flines (else)?
    else = <nl> <'  '>* <'else'> <W*> <':'> <W*> flines
    defn = <'def '> <W*> Aname <W*> <'('> args <')'> <W*> <':'>  flines
    args = arg? (<W*> <','> <W*> arg)* <W*>
    <arg> = name
    flines = (<nl> <'  '>+ (return | exp | assign | defn))+
    return = <'return '> <W*> exp
    assign = Aname <W*> <'='> <W*> exp
    Aname = name
    <exp> = A|M|D|Dp|Ap|Mp|Rname|fct|if
    fct = Fname <W*> <'('> <W*> exp? (<W*> <','> <W*> exp)* <W*> <')'>
    Fname = fct | name | namep
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
    keywords = 'return' | 'def' | 'if' | 'else'
    <name> = !keywords #'[a-zA-Z]\\w*'
    <nl> = '\n'
    "))

(defn get-from-mem
  [m m-lvl elem]
  ;; m = memories
  ;; m-lvl = current level of memory
  ;; elem = the name of the element
  (if-let [e (get-in m [m-lvl elem])]
    e
    (if (= m-lvl :global)
      (do
        #_(prn [:ERROR :does-not-exist elem])
        {:type :error
         :message (str "The value " elem " does not exist.")})
      (get-from-mem m (get-in m [m-lvl :__higher-mem__]) elem))))

(defn node-eval
  [m m-lvl [nn & nc]]
  ;; m = memory
  ;; m-lvl = current memory level
  ;; [nn nc] = node-name node-content
  #_(prn (str "  >>>  " nn "  " m-lvl "  " (count (keys m))))
  (case nn
    :S (reduce (fn [[m m-lvl _] n] (node-eval m m-lvl n))
               [m m-lvl nil]
               nc)
    :D [m m-lvl {:type :int
           :value (edn/read-string (first nc))}]
    :A (reduce
         (fn [[m m-lvl r] sub-n]
           (let [[Nm Nm-lvl Nr] (node-eval m m-lvl sub-n)]
             [Nm m-lvl {:type :int :value (+ (:value r) (:value Nr))}]))
         [m m-lvl {:type :int :value 0}]
         nc)
    :M (reduce
         (fn [[m m-lvl r] sub-n]
           (let [[Nm Nm-lvl Nr] (node-eval m m-lvl sub-n)]
             [Nm m-lvl {:type :int :value (* (:value r) (:value Nr))}]))
         [m m-lvl {:type :int :value 1}]
         nc)
    :assign (let [[aname expr] nc]
              (if (= (first aname) :Aname)
                (let [[new-m _ value] (node-eval m m-lvl expr)]
                  [(assoc-in new-m [m-lvl (second aname)] value)
                   m-lvl
                   nil])
                [m m-lvl [:error :assign]]))
    :Rname [m m-lvl (get-from-mem m m-lvl (first nc))]
    :defn (let [[aname args flines] nc]
            (if (and (= (first aname) :Aname)
                     (= (first args) :args)
                     (= (first flines) :flines))
              [(assoc-in m [m-lvl (second aname)]
                         {:type :fct
                          :args (rest args)
                          :flines (rest flines)
                          :m-lvl m-lvl
                          })
               m-lvl nil]
              [m [:error :defn]]))
    :Fname (node-eval m m-lvl [:fct (get-in m [m-lvl (first nc) :flines])])
    :fct (let [f-name-or-def? (->> nc
                                   first
                                   second)
               [m m-lvl f] (let [fsaved (get-from-mem m m-lvl f-name-or-def?)]
                             (if (= (:type fsaved) :fct)
                               [m m-lvl fsaved]
                               (node-eval m m-lvl f-name-or-def?)))
               argsName (:args f)
               flines (:flines f)
               current-m-name (get f :m-lvl m-lvl)
               [m _ args] (reduce
                            (fn [[m m-lvl prevargs] n]
                              (let [[nm _ argval] (node-eval m m-lvl n)]
                                [nm m-lvl (conj prevargs argval)]))
                            [m m-lvl []]
                            (rest nc))
               args (->> args
                         (map (fn [argN argV] [argN argV]) argsName))
               local-m-name (java.util.UUID/randomUUID)
               m-in-fct (reduce (fn [mm [argN argV]]
                                  (assoc mm argN argV))
                                {:__higher-mem__ current-m-name}
                                args)
               new-m (assoc m local-m-name m-in-fct)
               check (and (= (->> nc first first) :Fname)
                          (= (count argsName) (count (rest nc))))]
           (if check
             (loop [FLINES flines
                    new-m new-m]
               (if (empty? FLINES)
                 [new-m m-lvl [:error :fct :noreturn]]
                 (let [[L & FLINES] FLINES]
                   (if (= (first L) :return)
                     (let [res-n-e (node-eval new-m local-m-name (second L))]
                       (assoc res-n-e 1 m-lvl))
                     (let [res-n-e (node-eval new-m local-m-name L)]
                       (recur FLINES (first res-n-e))
                       )
                     ))))
             [new-m m-lvl [:error :fct :args-or-others]]))
  ))

#_(defn run
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
          outp (node-eval {} :global (ope inp))
          outpl (last outp)]
      #_(println)
      #_(println outp)
      #_(println outpl)
      outpl)
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
 #_#_   0 (run)
    1 (let [f (first args)]
        (if (or (= f "-u") (= f "-h"))
          (usage)
          (run-file f)))
    (usage)))
