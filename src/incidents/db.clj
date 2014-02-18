(ns incidents.db
  "Usage: just swap! the db to save data to memory.
  To persist to disk, just call (save-data!)
  To read from disk, just (read-data!)
  You can supply args to read/write from those."

  (:import java.io.File)
  (:require [clojure.edn :as edn]
            [utilza.repl :as urepl]
            [taoensso.timbre :as log]
            [environ.core :as env]))

;; Why not keep it simple, like this:
;;    http://www.brandonbloom.name/blog/2013/06/26/slurp-and-spit/


(defonce db (atom {}))
(defonce save-agent (agent nil))



(defn save-data!
  "With no args, saves to :db-filename saved in env.
   With one arg, saves to the path/filename specified."
  ([]
     (-> env/env :db-filename save-data!))
  ([dbfilename]
     (binding [*print-length* 10000000 *print-level* 10000000]
       (let [tmpfile (str dbfilename ".tmp")]
         (send-off save-agent
                   (fn [_]
                     (spit tmpfile (prn-str @db))
                     (.renameTo (File. tmpfile) (File. dbfilename))))))))


(defn read-data!
  "With no args, reads from :db-filename saved in env.
   With one arg, reads from the path/filename specified."
  ([]
     (->> env/env :db-filename read-data!))
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
      (log/info "Loading db first." (:db-filename env/env))
      (read-data!)
      (log/info "DB loaded (presumably)"))))


(comment




  
  )
