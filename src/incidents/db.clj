(ns incidents.db
  (:import java.io.File)
  (:require [clojure.edn :as edn]
            [environ.core :as env]))

;; Why not keep it simple, like this:
;;    http://www.brandonbloom.name/blog/2013/06/26/slurp-and-spit/


(defonce db (atom {}))
(defonce save-agent (agent nil))



(defn save-data
  []
  (let [dbfilename (:db-filename env/env)
        tmpfile (str dbfilename ".tmp")]
    (send-off save-agent
              (fn [_]
                (spit tmpfile (prn-str @db))
                (.renameTo (File. tmpfile) (File. dbfilename))))))


(defn read-data
  []
  (reset! db (->> env/env :db-filename slurp edn/read-string)))

;; Usage: just swap! the db to save data to memory.
;; To persist to disk, just call (save-data)
;; To read from disk, just (read-data)