(ns incidents.db
  "Usage: just swap! the db to save data to memory.
  To persist to disk, just call (save-data)
  To read from disk, just (read-data)
  You can supply args to read/write from those."

  (:import java.io.File)
  (:require [clojure.edn :as edn]
            [utilza.repl :as urepl]
            [environ.core :as env]))

;; Why not keep it simple, like this:
;;    http://www.brandonbloom.name/blog/2013/06/26/slurp-and-spit/


(defonce db (atom []))
(defonce save-agent (agent nil))



(defn save-data
  "With no args, saves to :db-filename saved in env.
   With one arg, saves to the path/filename specified."
  ([]
     (-> env/env :db-filename save-data))
  ([dbfilename]
     (let [tmpfile (str dbfilename ".tmp")]
       (send-off save-agent
                 (fn [_]
                   (spit tmpfile (prn-str @db))
                   (.renameTo (File. tmpfile) (File. dbfilename)))))))


(defn read-data
  "With no args, reads from :db-filename saved in env.
   With one arg, reads from the path/filename specified."
  ([]
     (->> env/env :db-filename read-data))
  ([dbfilename]
     (reset! db (->> dbfilename slurp edn/read-string))))





(comment

  ;;; Example search through the db. Note some-> to handle nil descriptions (happens)
  (->> @db
       (filter #(some-> % :description (.contains "Canyon")))
       (sort-by :time)
       (urepl/massive-spew "/tmp/output.edn"))

  )
