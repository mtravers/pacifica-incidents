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
            [org.parkerici.multitool.core :as u]
            [org.parkerici.multitool.cljcore :as ju]
            [environ.core :as env])
  (:import java.util.Date))

;;; Key within s3://incidents
(def save-file "db/latest.edn") ;TODO probably want to save versions

;;; The in-memory db
(defonce db (atom {}))

(defn save-data-local!
  [f]
  (ju/schppit f @db))

;;; TODO no real reasons these have to go through files
(defn save-data!
  []
  (log/info "Saving db")
  (let [local (fs/temp-file "db")]
    (save-data-local! local)
    (aws/file->s3 local save-file)))

(defn initialize!
  []
  (reset! db {})
  (save-data!))

(defn read-data!
  []
  (let [local (fs/temp-file "db")]
    (aws/s3->file save-file local)
    (reset! db (ju/read-from-file local))))

;;; More general
(defn with-db [f args]
  (read-data!)
  (apply swap! db f args)
  (save-data!))

(defn update! [f & args]
  (with-db f args))


;;; Stuff below here is from old system and may not be needed any more


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


(defn files []
  (-> @db
      :files
      vals))

(defn entry-inst [date e]
  (assoc e :inst (u/ignore-errors (java.util.Date. (str date " " (:date e))))))
  
(defn unified-entries
  []
  (mapcat (fn [{:keys [entries date]}]
            (map (partial entry-inst date) entries))
          (files)))

(defn max-time
  []
  (u/max-by identity (map :inst (unified-entries))))

(defn min-time
  []
  (u/min-by identity (map :inst (unified-entries))))

