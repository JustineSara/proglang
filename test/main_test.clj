(ns main-test
  (:require [clojure.test :refer [deftest is are]]
            [main :as m]))

(deftest istrue
  (is (= 1 1)))

(deftest step1
  (is (= [:S [:exp [:D "3"]]]
         (m/ope "3")))
  (is (= [:S [:exp [:A [:D "1"] [:D "1"]]]]
         (m/ope "1+1")))
  (is (= [:S [:exp [:A [:D "1"] [:D "2"]]]]
        (m/ope "1 + 2")))
  (is (= [:S [:exp [:M [:D "2"] [:D "2"] [:D "2"]]]]
         (m/ope "2 * 2*2")))
  (is (= [:S [:exp [:A [:M [:D "2"] [:D "1"]] [:D "1"]]]]
         (m/ope "2 * 1 + 1"))))

(deftest step2
  (are [text res] (= res (m/ope text))
    "2*1+1" [:S [:exp [:A [:M [:D "2"] [:D "1"]] [:D "1"]]]]
    "2*(1+1)" [:S [:exp [:M [:D "2"] [:A [:D "1"] [:D "1"]]]]]
    "(1+1)*(2+1+1)" [:S [:exp [:M [:A [:D "1"] [:D "1"]] [:A [:D "2"] [:D "1"] [:D "1"]]]]]
    "(1 + (2 * (3+1)))*3" [:S [:exp [:M [:A [:D "1"] [:M [:D "2"] [:A [:D "3"] [:D "1"]]]] [:D "3"]]]]
    "(1+2)*3" [:S [:exp [:M [:A [:D "1"] [:D "2"]] [:D "3"]]]]
    "(2)+2" [:S [:exp [:A [:D "2"] [:D "2"]]]]))

(deftest step3
  (are [text res] (= res (last (m/opeeval text {})))
    "2*1+1" [:result 3]
    "2*(1+1)" [:result 4]
    "(1+1)*(2+1+1)" [:result 8]
    "(1 + (2 * (3+1)))*3" [:result 27]
    "(1+2)*3" [:result 9]
    "(2)+2" [:result 4]))

(deftest step6
  (are [text res] (= res (m/opeeval text {}))
    "2+2\n3+3" [[:result 4] [:result 6]]))

(deftest step7
  (are [text res] (= res (m/ope text))
    "a=2" [:S [:assign [:Aname "a"] [:exp [:D "2"]]]]
    "2+a1" [:S [:exp [:A [:D "2"] [:Rname "a1"]]]])
  (are [text res] (= res (m/opeeval text {}))
    "a=2\n2+a" [[:mem "a" 2] [:result 4]]
    )
  (is (= (m/run-file "myprog/p2") 8)))
