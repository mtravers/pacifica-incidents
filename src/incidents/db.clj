(ns incidents.db
  "Usage: just swap! the db to save data to memory.
  To persist to disk, just call (save-data!)
  To read from disk, just (read-data!)
  You can supply args to read/write from those."

  (:require [incidents.pgdb :as pgdb]
            [clojure.edn :as edn]
            [cheshire.core :as json]
            [utilza.repl :as urepl]
            [taoensso.timbre :as log]
            [environ.core :as env])
  (:import java.util.Date))

;; Why not keep it simple, like this:
;;    http://www.brandonbloom.name/blog/2013/06/26/slurp-and-spit/


(defonce db (atom {}))
(defonce save-agent (agent nil))



(defn save-data! []
  (pgdb/save! @db))

(defn read-data!
  "With no args, reads from postgres
   With one arg, reads from the path/filename specified."
  ([]
     (reset! db (pgdb/read!))
     )
  ([dbfilename]
     (reset! db (->> dbfilename slurp edn/read-string))
     ;; Don't return the whole db so as not to crash emacs.
     nil))

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
