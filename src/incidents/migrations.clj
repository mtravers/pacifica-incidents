(ns incidents.migrations
  (:require [incidents.db :as db]))

;; THese things are migrations, but not really, because
;; this is a database, but not really.


(defn- convert-from-old-db-to-new!
  "DESTRUCTIVE function, not needed anymore."
  []
  (reset! db/db {})
  (doseq [{:keys [id] :as item} (->> "/tmp/db-as-seq.edn"
                                     slurp
                                     clojure.edn/read-string)]
    (swap! db/db  #(assoc % id item)))
  (db/save-data!))



(defn- fix-stupid-pdfs!
  "One-off function to fix bad data"
  []
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :disposition))]
    (swap! db/db (fn [db] (update-in db [id :disposition] parse/fix-stupid-pdf))))
  (db/save-data!))




(comment
  
  )
