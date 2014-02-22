(ns incidents.core-test
  (:require [clojure.test :refer :all]
            [incidents.scrape :as scrape]
            [incidents.core :refer :all]))

(comment

  (scrape/fix-malformed-url "http:/www.pacificaindex.com/policelogs/PPDdailymediabulletin2014-02-18.pdf")

  )