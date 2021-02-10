(ns incidents.db
  "Usage: just swap! the db to save data to memory.
  To persist to disk, just call (save-data!)
  To read from disk, just (read-data!)
  You can supply args to read/write from those."
  (:require [clojure.edn :as edn]
            [cheshire.core :as json]
            [utilza.repl :as urepl]
            [taoensso.timbre :as log]
            [incidents.aws :as aws]
            [me.raynes.fs :as fs]
            [org.parkerici.multitool.cljcore :as ju]
            [environ.core :as env])
  (:import java.util.Date))

;;; Key within s3://incidents
(def save-file "db/latest.edn") ;TODO probably want to save versions

;;; The in-memory db
(defonce db (atom {}))

;;; TODO no real reasons these have to go through files
(defn save-data!
  []
  (let [local (fs/temp-file "db")]
    (ju/schppit local @db)
    (aws/file->s3 local save-file)))

(defn read-data!
  []
  (let [local (fs/temp-file "db")]
    (aws/s3->file save-file local)
    (reset! db (ju/read-from-file local))))

(defn update-record
  "Returns a function to update the db by applying f to the record at id.
   Suitable for use with swap!"
  [id f]
  (fn [db]
    (update-in db [id] f)))

(defn db-init []
  (if (< 0 (count @db))
    (log/warn "Cowardly refusing to load db, it looks like it's already loaded")
    (do
      (log/info "Loading db first.")
      (read-data!)
      (log/info "DB loaded (presumably)"))))


(defn recover-from-backup
  "Takes url to disk file or web site, and forces the current db to match it"
  [url]
  (reset! db
          (into {}
                (for [{:keys [id time] :as rec}
                      (-> url
                          slurp
                          (json/decode true))]
                  [id (assoc rec :time (Date. time))]))))

(comment




  
  )
