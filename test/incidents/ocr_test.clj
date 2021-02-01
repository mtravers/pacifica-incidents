(ns incidents.ocr-test
  (:import org.joda.time.DateTime
           java.io.File)
  (:require [clojure.test :refer :all]
            [utilza.repl :as urepl]
            [clojure.edn :as edn]
            [taoensso.timbre :as log]
            [incidents.ocr :refer :all]))



(deftest basic-parse
  (testing "simple ocr parse test"
    (is  (= (->> "resources/testdata/textract-output.js"
                 read-file
                 parse-table
                 entries
                 rest
                 (map parse-entry))
            (->> "resources/testdata/textstract-parsed.edn"
                 slurp
                 edn/read-string)))))




