(ns incidents.pgdb
  (:require [alandipert.enduro :as e]
;Out of Order            [alandipert.enduro.pgsql :as pg]
;            [clojure.java.jdbc :as sql]

            [clojure.edn :as edn]
            [utilza.repl :as urepl]
            [taoensso.timbre :as log]
            [environ.core :as env]))

(defn db-config []
  (or (System/getenv "DATABASE_URL") "postgresql://localhost:5432/postgres"))

#_
(def db
  (delay
   (pg/postgresql-atom
    {}
    (db-config)
    "enduro")))

(def db-file "enduro.db")

(def db
  (delay
   (e/file-atom
    {}
    db-file)))

(defn save! [data]
  (e/reset! @db data)
  )

(defn read! []
  @@db
  )


