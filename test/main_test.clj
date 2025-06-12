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
  (are [text res] (= res (m/opeeval text))
    "2*1+1" 3
    "2*(1+1)" 4
    "(1+1)*(2+1+1)" 8
    "(1 + (2 * (3+1)))*3" 27
    "(1+2)*3" 9
    "(2)+2" 4))
