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
  "These need to be elsewhere"
  []
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :description))]
    (swap! db/db (fn [db] (assoc-in db [id :address] (some-> db (get id) :description geo/find-address)))))
  (db/save-data!))


(defn- strip-stupid-string-geos
  "I fucked up and was dumping :address into :geo instead of :geo, doh. This backs that error out"
  []
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :geo))]
    (swap! db/db (db/update-record id (fn [rec] (update-in rec [:geo]
                                                           #(if (= java.lang.String (type %))
                                                              nil
                                                              %))))))
  (db/save-data!))


(defn- fix-overly-verbose-geos
  "Too much crap. Remove."
  []
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :geo :geometry :location map?))]
    (swap! db/db (db/update-record id #(assoc % :geo (-> % :geo :geometry :location)))))
  (db/save-data!))


(comment

  (fix-overly-verbose-geos)


  
  )
