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

  (db/read-data!)

  ;; cron job 1:
  ;; for all dates
  ;; check the db
  ;; get the pdf if it isn't there
  ;; parse the pdf and save to db


  ;; cron job 2
  ;; (geo/update-geos)

  )


(comment

  )




(defn- convert-from-old-db-to-new
  "DESTRUCTIVE function, not needed anymore."
  []
  (reset! db/db {})
  (doseq [{:keys [id] :as item} (->> "/tmp/db-as-seq.edn"
                                     slurp
                                     clojure.edn/read-string)]
    (swap! db/db (fn [db]
                   (assoc db id item))))
  (db/save-data!))





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

  (db/save-data!)


  
  )




