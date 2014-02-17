(ns incidents.migrations
  (:require [incidents.db :as db]
            [incidents.geo :as geo]
            [incidents.parse :as parse]))

;; THese things are migrations, but not really, because
;; this is a database, but not really.


(defn- convert-from-old-db-to-new!
  "DESTRUCTIVE function, not needed anymore."
  []
  (db/save-data! "/tmp/old-seq.db")
  (reset! db/db {})
  (doseq [{:keys [id] :as item} (->> "/tmp/db-as-seq.edn"
                                     slurp
                                     clojure.edn/read-string)]
    (swap! db/db  #(assoc % id item)))
  (db/save-data!))



(defn- fix-stupid-pdfs!
  "One-off function to fix bad data"
  []
  (db/save-data! "/tmp/old-bad-pdf-dispositions.db")
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :disposition))]
    (swap! db/db (fn [db] (update-in db [id :disposition] parse/fix-stupid-pdf))))
  (db/save-data!))



(defn- pull-out-addresses!
  []
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :description))]
    (swap! db/db (fn [db] (assoc-in db [id :address] (-> db (get id) :description geo/find-address)))))
  (db/save-data!))

(comment

  (future (pull-out-addresses!))
  
  )
