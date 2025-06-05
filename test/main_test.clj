(ns main-test
  (:require [clojure.test :refer [deftest is]]
            [main :as m]))

(deftest istrue
  (is (= 1 1)))

(deftest step1
  (is (= [:S [:A [:D "1"]] "+" [:A [:D "1"]]]
         (m/ope "1+1")))
  (is (= [:S [:A [:D "1"]] "+" [:A [:D "2"]]]
        (m/ope "1 + 2")))
  (is (= [:S [:A [:D "2"] "*" [:D "2"] "*" [:D "2"]]]
         (m/ope "2 * 2*2")))
  (is (= [:S [:A [:D "2"] "*" [:D "1"]] "+" [:A [:D "1"]]]
         (m/ope "2 * 1 + 1"))))
