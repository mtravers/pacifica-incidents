(ns incidents.db
  (:import java.io.File))

;; Why not keep it simple, like this:
;;    http://www.brandonbloom.name/blog/2013/06/26/slurp-and-spit/


(defonce db (atom {}))
(defonce save-agent (agent nil))


;; TODO: put in env
(def dbfilename "/tmp/incidents.db")

(defn save-data
  []
  (let [tmpfile (str dbfilename ".tmp")]
    (send-off save-agent
              (fn [_]
                (spit tmpfile (prn-str @db))
                (.renameTo (File. tmpfile) (File. dbfilename))))))


;; Usage: just swap! the db to save data to memory.
;; To persist to disk, just call save-data