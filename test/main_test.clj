(ns main-test
  (:require [clojure.test :refer [deftest is are]]
            [main :as m]))

(deftest istrue
  (is (= 1 1)))

(deftest step1
  (is (= [:S [:D "3"]]
         (m/ope "3")))
  (is (= [:S [:A [:D "1"] [:D "1"]]]
         (m/ope "1+1")))
  (is (= [:S [:A [:D "1"] [:D "2"]]]
        (m/ope "1 + 2")))
  (is (= [:S [:M [:D "2"] [:D "2"] [:D "2"]]]
         (m/ope "2 * 2*2")))
  (is (= [:S [:A [:M [:D "2"] [:D "1"]] [:D "1"]]]
         (m/ope "2 * 1 + 1"))))

(deftest step2
  (are [text res] (= res (m/ope text))
    "2*1+1" [:S [:A [:M [:D "2"] [:D "1"]] [:D "1"]]]
    "2*(1+1)" [:S [:M [:D "2"] [:A [:D "1"] [:D "1"]]]]
    "(1+1)*(2+1+1)" [:S [:M [:A [:D "1"] [:D "1"]] [:A [:D "2"] [:D "1"] [:D "1"]]]]
    "(1 + (2 * (3+1)))*3" [:S [:M [:A [:D "1"] [:M [:D "2"] [:A [:D "3"] [:D "1"]]]] [:D "3"]]]
    "(1+2)*3" [:S [:M [:A [:D "1"] [:D "2"]] [:D "3"]]]
    "(2)+2" [:S [:A [:D "2"] [:D "2"]]]))

(deftest step3
  (are [text res] (= res (m/opeeval text {}))
    "2*1+1" [:result 3]
    "2*(1+1)" [:result 4]
    "(1+1)*(2+1+1)" [:result 8]
    "(1 + (2 * (3+1)))*3" [:result 27]
    "(1+2)*3" [:result 9]
    "(2)+2" [:result 4]))

(deftest step6
  (are [text res] (= res (m/opeeval text {}))
    "2+2\n3+3" [:result 4 6]))

(deftest step7
  (are [text res] (= res (m/ope text))
    "a=2" [:S [:assign [:Aname "a"] [:D "2"]]]
    "2+a1" [:S [:A [:D "2"] [:Rname "a1"]]])
  (are [text res] (= res (m/opeeval text {}))
    "a=2\n2+a" [:result [:mem "a" 2] 4]
    )
  (is (= (m/run-file "myprog/p2") 8)))

(deftest step 8
  (are [text res] (= res (m/ope text))
    "def add2(a):\n  return a+ 2\n" [:S [:defn [:Aname "add2"] [:args "a"] [:flines [:return [:A [:Rname "a"] [:D "2"]]]]]]
    "def give2():\n  return 2\n" [:S [:defn [:Aname "give2"] [:args ] [:flines [:return [:D "2"]]]]]
    "def add2(a):\n  b=a+2\n  return b\n" [:S [:defn [:Aname "add2"] [:args "a"] [:flines [:assign [:Aname "b"] [:A [:Rname "a"] [:D "2"]]] [:return [:Rname "b"]]]]]
    "def adds(a,b):\n  return a+ b\n" [:S [:defn [:Aname "adds"] [:args "a" "b"] [:flines [:return [:A [:Rname "a"] [:Rname "b"]]]]]]
    "add2(2)" [:S [:fct [:Rname "add2"] [:D "2"]]]
    )
  (are [text res] (= res (second (m/node-eval {} (m/ope text))))
    "def add2(a):\n  return a+ 2\n" nil
    "def add2(a):\n  return a+ 2\nadd2(40)" 42
    "def add2(a):\n  b=a+2\n  return b\nadd2(20*2)" 42
    "b=4\ndef add2(a):\n  b=a+2\n  return b\nadd2(10)" 12
    "b=4\ndef add2(a):\n  b=a+2\n  return b\nadd2(10)\nb" 4

    )
  )

(deftest grm-and-node-eval
  (are [text grm res] (= [grm res] [(m/ope text) (m/node-eval {} (m/ope text))])
    "2" [:S [:D "2"]] [{} 2]
    "2*1+1" [:S [:A [:M [:D "2"] [:D "1"]] [:D "1"]]] [{} 3]
    "2*(1+1)" [:S [:M [:D "2"] [:A [:D "1"] [:D "1"]]]] [{} 4]
    "a=2" [:S [:assign [:Aname "a"] [:D "2"]]] [{"a" 2} nil]
    "a=2\na*3" [:S [:assign [:Aname "a"] [:D "2"]] [:M [:Rname "a"] [:D "3"]]] [{"a" 2} 6]
    "a=1+1+1\n2*a" [:S [:assign [:Aname "a"] [:A [:D "1"][:D "1"][:D "1"]]] [:M [:D "2"] [:Rname "a"]]] [{"a" 3} 6]
    "def add2(a):\n  return a+2" [:S [:defn [:Aname "add2"] [:args "a"] [:flines [:return [:A [:Rname "a"] [:D "2"]]]]]] [{:fct {"add2" {:args ["a"] :flines [[:return [:A [:Rname "a"] [:D "2"]]]]}}} nil]
    "def add2(a):\n  return a+2\nadd2(40)" [:S [:defn [:Aname "add2"] [:args "a"] [:flines [:return [:A [:Rname "a"] [:D "2"]]]]] [:fct [:Rname "add2"] [:D "40"]]] [{:fct {"add2" {:args ["a"] :flines [[:return [:A [:Rname "a"] [:D "2"]]]]}}} 42]
  ))
