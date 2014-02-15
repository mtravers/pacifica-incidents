(ns incidents.core
  (:require [incidents.geo :as geo]
            [incidents.parse :as parse]
            [incidents.db :as db]
            [utilza.repl :as urepl]
            [incidents.dl :as dl]))



(comment
  ;; cron job 1:
  ;; for all dates
  ;; check the db
  ;; get the pdf if it isn't there
  ;; parse the pdf and save to db


  ;; cron job 2
  ;; for all dates
  ;; check that there are geos
  ;; if no geos, run the geos for that data


  )


(comment

  ;; quick geocode testing
  (->>  "resources/testdata/well-formed.txt"
        slurp
        parse/parse-pdf-text
        geo/add-geos
        (urepl/massive-spew "/tmp/output.edn"))

  )




