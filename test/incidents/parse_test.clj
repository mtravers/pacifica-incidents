(ns incidents.parse-test
  (:import org.joda.time.DateTime)
  (:require [clojure.test :refer :all]
            [instaparse.core :as ip]
            [utilza.repl :as urepl]
            [taoensso.timbre :as log]
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
  (is (= #inst "2014-02-04T20:03:00.000-08:00"
         (@#'incidents.parse/fix-time (DateTime. "2014-02-05T00:00:00.000Z")
                                      (DateTime. "1970-01-01T04:03:00.000Z"))))
  (testing "fixing in the structure")
  (is (=  [{:disposition "Log Note Only.",
            :description
            "Occurred on Monterey Rd, Pacifica. RP SOUNDS 1051 // REPORTING HIS BROTHER IS 1051 AND VIOLENT // BROTHER IS IN BEDROOM // RP CALLING FROM LIVING RM // NO WEAPONS // RP WILL OPEN DOOR FOR OFC'S",
            :id 140205007,
            :type "Dist Family",
            :time #inst "2014-02-04T17:27:00.000-08:00"}]
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
  (testing "Parsing a well-formed  PDF into the proper finished format")
  (is (= (->> "resources/testdata/well-formed.pdf"
              pdf-to-text
              parse-pdf-text)
         (->> "resources/testdata/well-formed-parsed.edn"
              slurp
              clojure.edn/read-string))))

(deftest stupid-pdfs
  (testing "*sigh*")
  (is (= "Report Taken. "
         (fix-stupid-pdf "Report Taken. PDF created with pdfFactory trial version www.pdffactory.com PACIFICA POLICE DEPARTMENT MEDIA BULLETIN DAILY --- Tuesday, August 14, 2012")))
  (is (= "Report Taken. "
         (fix-stupid-pdf "Report Taken. ")))
  (testing "some are empty, deal with that"
    (is (= nil
           (fix-stupid-pdf nil)))))



(defn- redo-tests
  "Cheating? Yeah. Do I care? No."
  []
  (urepl/massive-spew "resources/testdata/well-formed-parsed.edn"
                      (->> "resources/testdata/well-formed.pdf"
                           pdf-to-text
                           parse-pdf-text)))

(comment

  ;; (redo-tests) ;; evil, but necessary


  (run-tests)

  (first (clojure.string/split  "Report Taken. PDF created with pdfFactory trial version www.pdffactory.com PACIFICA POLICE DEPARTMENT MEDIA BULLETIN DAILY --- Tuesday, August 14, 2012"
                                #"PDF created with pdfFactory.*"))




  (pdf-to-text "/mnt/sdcard/tmp/policelogs/PPDdailymediabulletin2013-03-23.pdf")
  (pdf-to-text (clojure.java.io/input-stream "/mnt/sdcard/tmp/policelogs/PPDdailymediabulletin2013-03-23.pdf"))

  
  ;; WIN!
  (-> "http://localhost/PPDdailymediabulletin2013-03-23.pdf"
      java.net.URL.
      pdf-to-text)



  ;; one-offs that will work now! make into tests!
  "/mnt/sdcard/tmp/logs/policelogs/PPDdailymediabulletin2013-12-03.pdf"
  "/mnt/sdcard/tmp/logs/policelogs/PPDdailymediabulletin2014-01-12.pdf"

  ;; broken and will never work
  "/mnt/sdcard/tmp/logs/policelogs/5151-PPDdailymediabulletin(2012-09-17).pdf"
  "/mnt/sdcard/tmp/logs/policelogs/4995-PPDdailymediabulletin(2012-07-15).pdf"
  "/mnt/sdcard/tmp/logs/policelogs/4994-PPDdailymediabulletin(2012-07-14).pdf"
  
  
  ;; TODO: make test
  (@#'incidents.parse/parse-topline "00:06  Susp Circ 911                                         130327007")

  ;; check?

  
  
  )