(ns main-test
  (:require [clojure.test :refer [deftest is]]
            [main :as m]))

(deftest istrue
  (is (= 1 1)))

(deftest step1
  (is (= [:S [:A [:D "1"]] "+" [:A [:D "1"]]]
        (m/ope "1+1"))))
