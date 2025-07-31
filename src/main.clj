(ns main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [instaparse.core :as insta]))

(def new-gram
  (insta/parser
    "S = (INDENT) (if | else | defn | assign | return | exp | Q | Epsilon)
    INDENT = '  '*
    if = <'if '> <W*> exp <W*> <':'> <W*>
    else = <'else'> <W*> <':'> <W*>
    defn = <'def '> <W*> Aname <W*> <'('> <W*> args <W*> <')'> <W*> <':'> <W*>
    args = arg? (<W*> <','> <W*> arg)*
    <arg> = name
    return = <'return '> <W*> exp <W*>
    assign = Aname <W*> <'='> <W*> exp <W*>
    Aname = name
    <exp> = simple-op|Rname|fct|if
    <simple-op> = A|M|D|Dp|Ap|Mp
    fct = Fname <W*> <'('> <W*> exp? (<W*> <','> <W*> exp)* <W*> <')'> <W*>
    Fname = fct | name | namep
    <Ap> = <'('> A <')'>
    <Mp> = <'('> M <')'>
    <Dp> = <'('> D <')'>
    <namep> = <'('> name <')'>
    A = (elem|M|Mp|Ap) (<W*> <'+'> <W*> (elem|M|Mp|Ap))+
    M = (elem|Mp|Ap) (<W*> <'*'> <W*> (elem|Ap|Mp))+
    D = #'\\d+'
    <elem> = D | Dp | Rname
    Rname = name | namep
    W = #' '
    Q = 'quit!'|'q!'
    keywords = 'return' | 'def' | 'if' | 'else'
    <name> = !keywords #'[a-zA-Z]\\w*'
    <nl> = '\n'
    "))

(defn get-block
  [lines indent-lvl block]
  (loop [lines lines
         block block]
    (if (empty? lines)
      [nil indent-lvl block]
      (let [l (first lines)
            [_ INDENT sub-tree] (new-gram l)
            this-indent (dec (count INDENT))]
        (cond
          (nil? sub-tree) (recur (rest lines) block)
          (< this-indent indent-lvl) [lines (dec indent-lvl) block]
          (= this-indent indent-lvl)
          (case (first sub-tree)
            :if (let [[lines _ if-block] (get-block (rest lines) (inc indent-lvl) [:S])]
                  (recur lines (conj block (conj sub-tree if-block))))
            :else (let [isif? (= :if (first (last block)))
                        if-block (last block)
                        [lines _ else-block] (get-block (rest lines) (inc indent-lvl) sub-tree)]
                    (if isif?
                      (recur lines (assoc-in block [(dec (count block)) (count if-block)] else-block))))
            :defn (let [[lines _ def-block] (get-block (rest lines) (inc indent-lvl) [:flines])]
                    (recur lines (conj block (conj sub-tree def-block))))
            (recur (rest lines) (conj block sub-tree)))
          :else (recur (rest lines) (conj block [:ERROR "indent error" l]))
        )))))

(defn from-text-to-gram
  [text]
  (-> text
      s/split-lines
      (get-block 0 [:S])
      last))

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

(defn new-eval
  [m m-lvl [nn & nc]]
  ;; m = memory
  ;; m-lvl = current memory level
  ;; [nn nc] = node-name node-content
  #_(prn (str "  >>>  " nn "  " m-lvl "  " (count (keys m))))
  (case nn
    :S (reduce (fn [[m m-lvl _] n] (new-eval m m-lvl n))
               [m m-lvl nil]
               nc)
    :D [m m-lvl {:type :int
                 :value (edn/read-string (first nc))}]
    :A (reduce
         (fn [[m m-lvl r] sub-n]
           (let [[Nm Nm-lvl Nr] (new-eval m m-lvl sub-n)]
             [Nm m-lvl {:type :int :value (+ (:value r) (:value Nr))}]))
         [m m-lvl {:type :int :value 0}]
         nc)
    :M (reduce
         (fn [[m m-lvl r] sub-n]
           (let [[Nm Nm-lvl Nr] (new-eval m m-lvl sub-n)]
             [Nm m-lvl {:type :int :value (* (:value r) (:value Nr))}]))
         [m m-lvl {:type :int :value 1}]
         nc)
    :assign (let [[aname expr] nc]
              (if (= (first aname) :Aname)
                (let [[new-m _ value] (new-eval m m-lvl expr)]
                  [(assoc-in new-m [m-lvl (second aname)] value)
                   m-lvl
                   nil])
                [m m-lvl [:error :assign aname expr]]))
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
    :Fname (new-eval m m-lvl [:fct (get-in m [m-lvl (first nc) :flines])])
    :fct (let [f-name-or-def? (->> nc
                                   first
                                   second)
               [m m-lvl f] (let [fsaved (get-from-mem m m-lvl f-name-or-def?)]
                             (if (= (:type fsaved) :fct)
                               [m m-lvl fsaved]
                               (new-eval m m-lvl f-name-or-def?)))
               argsName (:args f)
               flines (:flines f)
               current-m-name (get f :m-lvl m-lvl)
               [m _ args] (reduce
                            (fn [[m m-lvl prevargs] n]
                              (let [[nm _ argval] (new-eval m m-lvl n)]
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
                     (let [res-n-e (new-eval new-m local-m-name (second L))]
                       (assoc res-n-e 1 m-lvl))
                     (let [res-n-e (new-eval new-m local-m-name L)]
                       (recur FLINES (first res-n-e))
                       )
                     ))))
             [new-m m-lvl [:error :fct :args-or-others]]))
    :if (let [[condition is-true is-false] nc
              evaluated-condition (last (new-eval m m-lvl condition))]
          (if (zero? (:value evaluated-condition))
            (new-eval m m-lvl is-true)
            (if (= (first is-false) :else)
              (new-eval m m-lvl (assoc is-false 0 :S))
              [m m-lvl nil])))
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
          outp (new-eval {} :global (from-text-to-gram inp))
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
