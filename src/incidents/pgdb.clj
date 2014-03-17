(ns incidents.pgdb
  (:require [alandipert.enduro :as e]
            [alandipert.enduro.pgsql :as pg]
            [clojure.java.jdbc :as sql])
  (:require [clojure.edn :as edn]
            [utilza.repl :as urepl]
            [taoensso.timbre :as log]
            [environ.core :as env]))

(defn db-config []
  (or (System/getenv "DATABASE_URL") "postgresql://localhost:5432/postgres"))

(def db
  (delay
   (pg/postgresql-atom
    {}
    (db-config)
    "enduro")))

(defn save! [data]
  (e/reset! @db data)
  )

(defn read! []
  @@db
  )


