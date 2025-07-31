(ns main-test
  (:require [clojure.test :refer [deftest is are]]
            [main :as m]))

(defn get-res
  [txt]
  (->> txt
       m/from-text-to-gram
       (m/new-eval {} 0)
       last
       :value))

(deftest istrue
  (is (= 1 1)))

(deftest step1
  (is (= [:S [:D "3"]]
         (m/from-text-to-gram "3")))
  (is (= [:S [:A [:D "1"] [:D "1"]]]
         (m/from-text-to-gram "1+1")))
  (is (= [:S [:A [:D "1"] [:D "2"]]]
         (m/from-text-to-gram "1 + 2")))
  (is (= [:S [:M [:D "2"] [:D "2"] [:D "2"]]]
         (m/from-text-to-gram "2 * 2*2")))
  (is (= [:S [:A [:M [:D "2"] [:D "1"]] [:D "1"]]]
         (m/from-text-to-gram "2 * 1 + 1"))))

(deftest step2
  (are [text res] (= res (m/from-text-to-gram text))
    "2*1+1" [:S [:A [:M [:D "2"] [:D "1"]] [:D "1"]]]
    "2*(1+1)" [:S [:M [:D "2"] [:A [:D "1"] [:D "1"]]]]
    "(1+1)*(2+1+1)" [:S [:M [:A [:D "1"] [:D "1"]] [:A [:D "2"] [:D "1"] [:D "1"]]]]
    "(1 + (2 * (3+1)))*3" [:S [:M [:A [:D "1"] [:M [:D "2"] [:A [:D "3"] [:D "1"]]]] [:D "3"]]]
    "(1+2)*3" [:S [:M [:A [:D "1"] [:D "2"]] [:D "3"]]]
    "(2)+2" [:S [:A [:D "2"] [:D "2"]]]))

(deftest step3
  (are [text res] (= res (->> text
                              m/from-text-to-gram
                              (m/new-eval {} :global)
                              last
                              :value))
    "2*1+1" 3
    "2*(1+1)" 4
    "(1+1)*(2+1+1)" 8
    "(1 + (2 * (3+1)))*3" 27
    "(1+2)*3" 9
    "(2)+2" 4))

(deftest step6
  (are [text res] (= res (->> text
                              m/from-text-to-gram
                              (m/new-eval {} :global)
                              last
                              :value))
    "2+2" 4
    "2+2\n3+3" 6))

(deftest step7
  (are [text res] (= res (m/from-text-to-gram text))
    "a=2" [:S [:assign [:Aname "a"] [:D "2"]]]
    "2+a1" [:S [:A [:D "2"] [:Rname "a1"]]])
  (are [text res] (= res (m/new-eval {} :global (m/from-text-to-gram text)))
    "a=2\n2+a" [{:global {"a" {:type :int :value 2}}} :global {:type :int :value 4}]
    )
  (is (= (m/run-file "myprog/p2") {:type :int :value 8})))

(deftest step8-1
  (are [text res] (= res (m/from-text-to-gram text))
    "def add2(a):\n  return a+ 2\n" [:S [:defn [:Aname "add2"] [:args "a"] [:flines [:return [:A [:Rname "a"] [:D "2"]]]]]]
    "def give2():\n  return 2\n" [:S [:defn [:Aname "give2"] [:args ] [:flines [:return [:D "2"]]]]]
    "def add2(a):\n  b=a+2\n  return b\n" [:S [:defn [:Aname "add2"] [:args "a"] [:flines [:assign [:Aname "b"] [:A [:Rname "a"] [:D "2"]]] [:return [:Rname "b"]]]]]
    "def adds(a,b):\n  return a+ b\n" [:S [:defn [:Aname "adds"] [:args "a" "b"] [:flines [:return [:A [:Rname "a"] [:Rname "b"]]]]]]
    "add2(2)" [:S [:fct [:Fname "add2"] [:D "2"]]]
    )
  (are [text res] (= res (nth (m/new-eval {} :global (m/from-text-to-gram text)) 2))
    "def add2(a):\n  return a+ 2\n" nil
    "def add2(a):\n  return a+ 2\nadd2(40)" {:type :int :value 42}
    "def add2(a):\n  b=a+2\n  return b\nadd2(20*2)" {:type :int :value 42}
    "b=4\ndef add2(a):\n  b=a+2\n  return b\nadd2(10)" {:type :int :value 12}
    "b=4\ndef add2(a):\n  b=a+2\n  return b\nadd2(10)\nb" {:type :int :value 4})
  (is (= (m/run-file "myprog/p-fct") {:type :int :value 4})))


(deftest grm-and-node-eval
  (are [text grm res] (= [grm res] [(m/from-text-to-gram text) (m/new-eval {} :global (m/from-text-to-gram text))])
    "2" [:S [:D "2"]] [{} :global {:type :int :value 2}]
    "2*1+1" [:S [:A [:M [:D "2"] [:D "1"]] [:D "1"]]] [{} :global {:type :int :value 3}]
    "2*(1+1)" [:S [:M [:D "2"] [:A [:D "1"] [:D "1"]]]] [{} :global {:type :int :value 4}]
    "a=2" [:S [:assign [:Aname "a"] [:D "2"]]] [{:global {"a" {:type :int :value 2}}} :global nil]
    "a=2\na*3"
    [:S [:assign [:Aname "a"] [:D "2"]] [:M [:Rname "a"] [:D "3"]]]
    [{:global {"a" {:type :int :value 2}}} :global {:type :int :value 6}]
    "a=1+1+1\n2*a"
    [:S [:assign [:Aname "a"] [:A [:D "1"][:D "1"][:D "1"]]] [:M [:D "2"] [:Rname "a"]]]
    [{:global {"a" {:type :int :value 3}}} :global {:type :int :value 6}]
    "def add2(a):\n  return a+2"
    [:S [:defn [:Aname "add2"] [:args "a"] [:flines [:return [:A [:Rname "a"] [:D "2"]]]]]]
    [{:global {"add2" {:type :fct :args ["a"] :flines [[:return [:A [:Rname "a"] [:D "2"]]]] :m-lvl :global}}} :global nil]
  ))

(deftest step8-mem
  (let [text "def add2(a):\n  return a+2\nadd2(40)"
        E-gram [:S [:defn [:Aname "add2"] [:args "a"] [:flines [:return [:A [:Rname "a"] [:D "2"]]]]] [:fct [:Fname "add2"] [:D "40"]]]
        C-gram (m/from-text-to-gram text)
        [C-m _ C-res] (m/new-eval {} :global C-gram)
        C-mem-global (:global C-m)
        m-keys (keys C-m)
        not-global-key (if (= :global (first m-keys)) (second m-keys) (first m-keys))
        C-mem-fct (get C-m not-global-key)
        E-res {:type :int :value 42}
        E-mem-global {"add2" {:type :fct :args ["a"] :flines [[:return [:A [:Rname "a"] [:D "2"]]]] :m-lvl :global}}
        E-mem-fct {:__higher-mem__ :global, "a" {:type :int, :value 40}}]
    (are [a b] (= a b)
      E-gram C-gram
      E-res C-res
      E-mem-global C-mem-global
      E-mem-fct C-mem-fct
    )))

(deftest NickTest
  ;; solving Nick's test
  (is (=
       (m/run-file "myprog/nick-fct")
       {:type :int :value 42})))

(deftest ifimplementation
  ;; testing if and if-then (no boolean)
  (are [txt gram] (= (m/from-text-to-gram txt) gram )
    "if 0 :\n  1" [:S [:if [:D "0"] [:S [:D "1"]]]]
    "if 0:\n  a=1\nelse:\n  a=2\na" [:S
                                     [:if [:D "0"]
                                          [:S [:assign [:Aname "a"] [:D "1"]]]
                                          [:else [:assign [:Aname "a"] [:D "2"]]]]
                                     [:Rname "a"]]
    "if 0:\n  if 0:\n    a=1\n  else:\n    a=2\nelse:\n  a=3"
    [:S [:if [:D "0"]
             [:S [:if [:D "0"]
                      [:S [:assign [:Aname "a"] [:D "1"]]]
                      [:else [:assign [:Aname "a"] [:D "2"]]]]]
             [:else [:assign [:Aname "a"] [:D "3"]]]]]
    "if 0:\n  if 0:\n    a=1\n  else:\n    a=2\n"
    [:S [:if [:D "0"]
             [:S [:if [:D "0"]
                      [:S [:assign [:Aname "a"] [:D "1"]]]
                      [:else [:assign [:Aname "a"] [:D "2"]]]]]]]
    "if 0:\n  if 0:\n    a=1\nelse:\n  a=3"
    [:S [:if [:D "0"]
             [:S [:if [:D "0"]
                      [:S [:assign [:Aname "a"] [:D "1"]]]]]
             [:else [:assign [:Aname "a"] [:D "3"]]]]]
    "if 0:\n  if 0:\n    a=1\n  else:\n    a=2\n  b=10\nelse:\n  b=20\na+b"
    [:S [:if
         [:D "0"]
         [:S
          [:if [:D "0"]
           [:S [:assign [:Aname "a"] [:D "1"]]]
           [:else [:assign [:Aname "a"] [:D "2"]]]]
          [:assign [:Aname "b"] [:D "10"]]]
         [:else [:assign [:Aname "b"] [:D "20"]]]]
        [:A [:Rname "a"] [:Rname "b"]]]
    "if 0:\n  if 0:\n    a=1\n  else:\n    a=2\n    b=10\nelse:\n  b=20\na+b"
    [:S [:if
         [:D "0"]
         [:S
          [:if [:D "0"]
           [:S [:assign [:Aname "a"] [:D "1"]]]
           [:else [:assign [:Aname "a"] [:D "2"]]
                  [:assign [:Aname "b"] [:D "10"]]]]]
         [:else [:assign [:Aname "b"] [:D "20"]]]]
        [:A [:Rname "a"] [:Rname "b"]]])
  (are [txt res] (= (:value (last (m/new-eval {} :global (m/from-text-to-gram txt)))) res)
    "if 1 :\n  1" 1
    "if 1:\n  a=1\nelse:\n  a=2\na" 1
    "if 0:\n  a=1\nelse:\n  a=2\na" 2
    "if 1:\n  if 1:\n    a=1\n  else:\n    a=2\nelse:\n  a=3\na" 1
    "if 1:\n  if 0:\n    a=1\n  else:\n    a=2\nelse:\n  a=3\na" 2
    "if 0:\n  if 1:\n    a=1\n  else:\n    a=2\nelse:\n  a=3\na" 3
    "if 1:\n  if 1:\n    a=1\n  else:\n    a=2\na" 1
    "if 1:\n  if 0:\n    a=1\n  else:\n    a=2\na" 2
    "if 0:\n  if 1:\n    a=1\n  else:\n    a=2\na" nil
    "if 1:\n  if 1:\n    a=1\n  else:\n    a=2\n  b=10\nelse:\n  b=20\na+b" 11
    "if 1:\n  if 0:\n    a=1\n  else:\n    a=2\n  b=10\nelse:\n  b=20\na+b" 12
    "a=0\nif 0:\n  if 0:\n    a=1\n  else:\n    a=2\n  b=10\nelse:\n  b=20\na+b" 20
    "a=0\nb=0\nif 1:\n  if 1:\n    a=1\n  else:\n    a=2\n    b=10\nelse:\n  b=20\na+b" 1
    "a=0\nb=0\nif 1:\n  if 0:\n    a=1\n  else:\n    a=2\n    b=10\nelse:\n  b=20\na+b" 12
    "a=0\nb=0\nif 0:\n  if 1:\n    a=1\n  else:\n    a=2\n    b=10\nelse:\n  b=20\na+b" 20)
  )

(deftest bool

  ;; test grammar
  (are [txt gram] (= (m/from-text-to-gram txt) gram)
    "True" [:S [:B "True"]]
    "False" [:S [:B "False"]]
    "if True:\n  1" [:S [:if [:B "True"] [:S [:D "1"]]]]
    )

  (are [txt res] (= (->> txt
                        m/from-text-to-gram
                        (m/new-eval {} 0)
                        last
                        :value)
                    res)
  ;; test eval of booleans
    "True" true
    "False" false
    "if 1:\n  True\nelse:\n  False" true
    "if 0:\n  True\nelse:\n  False" false
  ;; test booleans in ifs
    "if True:\n  1\nelse:\n  2" 1
    "if False:\n  1\nelse:\n  2" 2)

  ;; test the == operator
  (are [txt gram] (= (m/from-text-to-gram txt) gram)
    "1 == 1" [:S [:Bequ [:D "1"] [:D "1"]]]
    "2+2 == 2*2 == (1+1)*2" [:S [:Bequ
                                 [:A [:D "2"] [:D "2"]]
                                 [:M [:D "2"] [:D "2"]]
                                 [:M [:A [:D "1"] [:D "1"]] [:D "2"]]]]
    "True == (2 == (1+1))" [:S [:Bequ
                                [:B "True"]
                                [:Bequ
                                 [:D "2"]
                                 [:A [:D "1"] [:D "1"]]]]])
  (are [txt res] (= (->> txt
                         m/from-text-to-gram
                         (m/new-eval {} 0)
                         last
                         :value)
                    res)
    "1 == 1" true
    "1 ==2" false
    "2+2 == 2*2 == (1+1)*2" true
    "42 == 2*2 == (1+1)*2" false
    "2+2 == 42 == (1+1)*2" false
    "2+2 == 2*2 == 42" false
    "True == (2 == (1+1))" true
    "if 1+1==2:\n  1\nelse:\n  2" 1)
  )

(deftest returns
  (let [txt "def te():\n  return 1\n  return 2\nte()"]
    (is (= 1
           (->> txt
                m/from-text-to-gram
                (m/new-eval {} 0)
                last
                :value)))))

(deftest negativenumb
  (are [txt res] (= res (get-res txt))
    "4+-5" -1
    "-5*2+4" -6))
