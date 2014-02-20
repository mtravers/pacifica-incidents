(ns incidents.migrations
  (:require [incidents.db :as db]
            [taoensso.timbre :as log]
            [incidents.geo :as geo]
            [incidents.utils :as utils]
            [utilza.repl :as urepl]
            [incidents.scrape :as scrape]
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
    (log/debug "fixing " id)
    (swap! db/db  #(assoc % id item)))
  (db/save-data!))



(defn- fix-stupid-pdfs!
  "One-off function to fix bad data"
  []
  (db/save-data! "/tmp/old-bad-pdf-dispositions.db")
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :disposition))]
    (log/debug "fixing " id)
    (swap! db/db (fn [db] (update-in db [id :disposition] parse/fix-stupid-pdf))))
  (db/save-data!))



(defn- pull-out-addresses!
  "These need to be elsewhere"
  []
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :description))]
    (log/debug "fixing " id)
    (swap! db/db (fn [db] (assoc-in db [id :address] (some-> db (get id) :description geo/find-address)))))
  (db/save-data!))


(defn- strip-stupid-string-geos
  "I fucked up and was dumping :address into :geo instead of :geo, doh. This backs that error out"
  []
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :geo))]
    (log/debug "fixing " id)
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
    (log/debug "fixing " id)
    (swap! db/db (db/update-record id #(assoc % :geo (-> % :geo :geometry :location)))))
  (db/save-data!))




(defn- fix-non-numeric-keys
  "A bunch of these ended up as strings, not numbers. why?"
  []
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (string? id))]
    (log/debug "fixing " id)
    (swap! db/db (fn [db]
                   (let [num-id (Long/parseLong id)]
                     (-> db
                         (dissoc id)
                         (assoc num-id (assoc (get db id) :id num-id)))))))
  (db/save-data!))


(defn new-parsing-system
  "Use the new pdf parsing library"
  [file-dir]
  (doseq [f (->> file-dir
                 java.io.File.
                 .listFiles)
          :when (->> f .toString  (re-find #".*PPDdailymediabulletin.+.pdf"))]
    (scrape/fetch-and-add-to-db! f))
  (db/save-data!))



(defn- redo-slash-addresses
  "Migrate to Mike's improvement to slash address regexp"
  []
  (doseq [{:keys [id]} (utils/simple-contains  :address "/")
          :when (-> id nil? not)]
    (log/debug "fixing " id)
    (swap! db/db (db/update-record id (fn [{:keys [description] :as rec}]
                                        (->> description
                                             geo/find-address ;; Mike's new find-address with the / handling
                                             (assoc rec :address)
                                             geo/copy-or-fetch-geo ;; redo the geocoding now!
                                             )))))
  (db/save-data!))


(comment

  (def running-migration (future (redo-slash-addresses)))
  
  (future-cancel running-migration)

  (future-done? running-migration)


  
  )
