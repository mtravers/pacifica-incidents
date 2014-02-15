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
  (is (= {:stuff
          "Occurred on Monterey Rd, Pacifica. RP SOUNDS 1051 // REPORTING HIS BROTHER IS 1051 AND VIOLENT// BROTHER IS IN BEDROOM // RP CALLING FROM LIVING RM // NO WEAPONS // RP WILL OPEN DOORFOR OFC'S Disposition: Log Note Only.",
          :id 140205007,
          :type "Dist Family",
          :time (DateTime. "1969-12-31T17:27:00.000-08:00")}
         (munge-rec
          [[:time (DateTime. "1970-01-01T01:27:00.000Z")]
           [:type "Dist Family"]
           [:id 140205007]
           [:stuff
            "Occurred on Monterey Rd, Pacifica. RP SOUNDS 1051 // REPORTING HIS BROTHER IS 1051 AND VIOLENT"]
           [:stuff
            "// BROTHER IS IN BEDROOM // RP CALLING FROM LIVING RM // NO WEAPONS // RP WILL OPEN DOOR"]
           [:stuff "FOR OFC'S Disposition: Log Note Only."]]))))

(comment

  (run-tests)

  )