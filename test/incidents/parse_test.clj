(ns incidents.parse-test
  (:import org.joda.time.DateTime)
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

(deftest munge-rec-test
  (testing "munging the descriptions")
  (is (= {:description
          "Occurred on Monterey Rd, Pacifica. RP SOUNDS 1051 // REPORTING HIS BROTHER IS 1051 AND VIOLENT // BROTHER IS IN BEDROOM // RP CALLING FROM LIVING RM // NO WEAPONS // RP WILL OPEN DOOR FOR OFC'S Disposition: Log Note Only.",
          :id 140205007,
          :type "Dist Family",
          :time (DateTime. "1969-12-31T17:27:00.000-08:00")}
         (@#'incidents.parse/munge-rec
          [[:time (DateTime. "1970-01-01T01:27:00.000Z")]
           [:type "Dist Family"]
           [:id 140205007]
           [:description
            "Occurred on Monterey Rd, Pacifica. RP SOUNDS 1051 // REPORTING HIS BROTHER IS 1051 AND VIOLENT"]
           [:description
            "// BROTHER IS IN BEDROOM // RP CALLING FROM LIVING RM // NO WEAPONS // RP WILL OPEN DOOR"]
           [:description "FOR OFC'S Disposition: Log Note Only."]]))))


(deftest disposition-test
  (testing "yanking the dispositions")
  (is (=  {:disposition "Log Note Only.",
           :description
           "Occurred on Monterey Rd, Pacifica. RP SOUNDS 1051 // REPORTING HIS BROTHER IS 1051 AND VIOLENT // BROTHER IS IN BEDROOM // RP CALLING FROM LIVING RM // NO WEAPONS // RP WILL OPEN DOOR FOR OFC'S",
           :id 140205007,
           :type "Dist Family",
           :time (DateTime. "1969-12-31T17:27:00.000-08:00")}
          (@#'incidents.parse/yank-disposition
           {:description
            "Occurred on Monterey Rd, Pacifica. RP SOUNDS 1051 // REPORTING HIS BROTHER IS 1051 AND VIOLENT // BROTHER IS IN BEDROOM // RP CALLING FROM LIVING RM // NO WEAPONS // RP WILL OPEN DOOR FOR OFC'S Disposition: Log Note Only.",
            :id 140205007,
            :type "Dist Family",
            :time (DateTime. "1969-12-31T17:27:00.000-08:00")}))))


(deftest time-fix-test
  (testing "time fixing")
  (is (= (DateTime. "2014-02-04T20:03:00.000-08:00")
         (@#'incidents.parse/fix-time (DateTime. "2014-02-05T00:00:00.000Z")
                                      (DateTime. "1970-01-01T04:03:00.000Z"))))
  (testing "fixing in the structure")
  (is (=  [{:disposition "Log Note Only.",
            :description
            "Occurred on Monterey Rd, Pacifica. RP SOUNDS 1051 // REPORTING HIS BROTHER IS 1051 AND VIOLENT // BROTHER IS IN BEDROOM // RP CALLING FROM LIVING RM // NO WEAPONS // RP WILL OPEN DOOR FOR OFC'S",
            :id 140205007,
            :type "Dist Family",
            :time (DateTime. "2014-02-04T17:27:00.000-08:00")}]
          (@#'incidents.parse/fix-times
           {:date (DateTime. "2014-02-05T00:00:00.000Z")
            :recs
            [{:disposition "Log Note Only.",
              :description
              "Occurred on Monterey Rd, Pacifica. RP SOUNDS 1051 // REPORTING HIS BROTHER IS 1051 AND VIOLENT // BROTHER IS IN BEDROOM // RP CALLING FROM LIVING RM // NO WEAPONS // RP WILL OPEN DOOR FOR OFC'S",
              :id 140205007,
              :type "Dist Family",
              :time (DateTime. "1970-01-01T01:27:00.000Z")}]}))))


(deftest all-parsing
  (testing "Parsing a well-formed pdftotext'ed PDF into the proper finished format")
  (is (= (->> "resources/testdata/well-formed.txt"
              slurp
              parse-pdf-text)
         (->> "resources/testdata/well-formed-parsed.edn"
              slurp
              clojure.edn/read-string)))
  (testing "Parsing a poorly-formed pdftotext'ed PDF into the proper finished format")
  (is (= (->> "resources/testdata/poorly-formed.txt"
              slurp
              parse-pdf-text)
         (->> "resources/testdata/poorly-formed-parsed.edn"
              slurp
              clojure.edn/read-string))))

(comment

  (run-tests)

  )