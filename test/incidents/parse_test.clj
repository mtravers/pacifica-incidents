(ns incidents.parse-test
  (:require [clojure.test :refer :all]
            [instaparse.core :as ip]
            [incidents.parse :refer :all]))

(deftest basic-parse
  (testing "just a dead simple plumbing test"
    (is  (= (ip/parse
             (ip/parser (slurp "resources/test.bnf"))
             "aaaaabbbaaaabb")
            [:S
             [:AB
              [:A "a" "a" "a" "a" "a"]
              [:B "b" "b" "b"]]
             [:AB
              [:A "a" "a" "a" "a"]
              [:B "b" "b"]]]))))


(comment

  (run-tests)

  )