(ns incidents.core
  (:require [incidents.geo :as geo]
            [incidents.parse :as parse]
            [incidents.db :as db]
            [taoensso.timbre :as log]
			[environ.core :as env]
            [utilza.repl :as urepl]
            [incidents.dl :as dl])
  (:gen-class))


(log/set-config! [:appenders :spit :enabled?] true)
(log/set-config! [:shared-appender-config :spit-filename] (:log-filename env/env))




(defn -main
  []
  (future
    (log/info "Compiling namespace, loading db first.")
    (db/read-data!)
    (log/info "DB loaded (presumably)")))


(comment

  ;; cron job 1:
  ;; for all dates
  ;; check the db
  ;; get the pdf if it isn't there
  ;; parse the pdf and save to db


  ;; cron job 2
  ;; (geo/update-geos)

  )





(comment
  ;; attempt to parse everthang

  ;; TODO: Do this as part of the downloading operation

  (reset! db/db {})
  (doseq [f (->> "/mnt/sdcard/tmp/policelogs"
                 java.io.File.
                 .listFiles)
          :when (-> f .toString (.endsWith ".txt"))]
    (do
      (log/info (.toString f))
      (->> f
           slurp
           parse/parse-pdf-text
           (doseq [{:keys [id] :as item} items]
             (swap! db/db (fn [db]
                            (assoc db id item)))))))

  (count @db/db)

  (db/save-data! "/tmp/backup.db")

  (db/read-data! "/tmp/backup.db")
  
  )




