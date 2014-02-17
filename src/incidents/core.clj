(ns incidents.core
  (:require [incidents.geo :as geo]
            [incidents.parse :as parse]
            [incidents.db :as db]
            [taoensso.timbre :as log]
			[environ.core :as env]
            [utilza.repl :as urepl]
            [incidents.dl :as dl])
  (:gen-class))

;; IMPORTANT: This bare exec is here to dothis FIRST before running anything, at compile time
(log/merge-config! (:timbre-config env/env))

(defn -main
  []
  (future
    (try
      (log/info "Loading db first." (:db-filename env/env))
      (db/read-data!)
      (log/info "DB loaded (presumably)")
      (catch Exception e
        (log/error e)))))


(comment

  ;; cron job 1:
  ;; for all dates
  ;; check the db
  ;; get the pdf if it isn't there
  ;; parse the pdf and save to db


  ;; cron job 2
  ;; (geo/update-geos)

  (-main)

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




