(ns incidents.core
  (:require [incidents.geo :as geo]
            [incidents.parse :as parse]
            [incidents.db :as db]
            [taoensso.timbre :as log]
			[environ.core :as env]
            [utilza.repl :as urepl]
            [incidents.dl :as dl]))

(log/set-config! [:appenders :spit :enabled?] true)
(log/set-config! [:shared-appender-config :spit-filename] (:log-filename env/env))

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

  ;; do this to geocode everything in the db
  ;; that doesn't already have a geo
  (doseq [item @db/db]
    (when (-> item :geo empty?)
      (swap! db/db geo/add-geo)))
  (db/save-data)
  


  )


(comment
  ;; attempt to parse everthang

  (reset! db/db [])
  (doseq [f (->> "/mnt/sdcard/tmp/policelogs"
                 java.io.File.
                 .listFiles)
          :when (-> f .toString (.endsWith ".txt"))]
    (do
      (log/info (.toString f))
      (->> f
           slurp
           parse/parse-pdf-text
           (swap! db/db concat))))

  (count @db/db)

  (db/save-data)


  
  
  )



