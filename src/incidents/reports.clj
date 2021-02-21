(ns incidents.reports
  (:import org.joda.time.DateTime
           org.joda.time.DateTimeZone)
  (:require [clojure.edn :as edn]
            [utilza.repl :as urepl]
            [incidents.db :as db]
            [incidents.utils :as utils]
            [incidents.geo :as geo]
            [clojure.core.reducers :as r]
            [environ.core :as env]))


(defn disposition-total
  [db]
  (utils/total-not-null-counts db :disposition))

(defn disposition-counts
  [db]
  (utils/key-set-counts db :disposition))

(defn types-total
  [db ]
  (utils/total-not-null-counts db :type))


(defn type-counts
  [db]
  (utils/key-set-counts db :type))

(defn geo-total
  [db ]
  (utils/total-not-null-counts db :geo))

(defn description-total
  [db]
  (utils/total-not-null-counts db :description))

(defn total-records
  [db]
  (->> db
       vals
       count))



(defn spot-check
  [db]
  (get db (-> db
              keys
              rand-nth)))


(defn missing-important-stuff
  [db]
  (let [total (total-records db)]
    (reduce (fn [a k]
              (assoc a k  (- total (utils/key-set-counts db k))))
            {}
            [:description :type :disposition])))


(defn stringify-date
  "Gets the dates in the hardcoded timezone of Pacific time.
  Because the server could be in a different timezone!"
  [d]
  (-> d
      (org.joda.time.DateTime. (org.joda.time.DateTimeZone/forID "America/Los_Angeles"))
      .toLocalDate
      .toString))

(defn unique-dates
  "Unique dates without timestamps in the db"
  [db]
  (reduce #(conj %1 %2)
          #{}
          (r/map #(-> %
                      :time
                      stringify-date)
                 (->> db
                      vals))))

(defn date-only-min-max
  "poorly named"
  [db]
  (->> (unique-dates db)
       sort
       ((juxt first last))
       (zipmap [:min :max])))

(defn days-total
  [db]
  (count (unique-dates db)))

(defn timestamps-min-max
  [_]
  {:min (db/min-time)
   :max (db/max-time)})

(defn  address-counts
  [db]
  (utils/key-set-counts db :address))

(defn total-addresses
  [db]
  (utils/total-not-null-counts db :address))

(defn  geo-counts
  [db]
  (utils/key-set-counts db :geo))

(defn botchy-geos
  [db]
  (for [[k v] (utils/key-set-counts db (comp type :geo))]
    [(str k) v]))


(defn most-recent-by-geo
  [db]
  (for [geo  (utils/all-keys db :geo)]
    (->> db
         vals
         (filter #(= (:geo %) geo))
         (sort-by :time)
         reverse
         first)))

(defn quick-status
  [db]
  {:total-incidents (total-records db)
   :total-types (types-total db)
   :total-dispositions (disposition-total db)
   :total-descriptions (description-total db)
   :total-geos (geo-total db)
   ;; :botchy-geos (botchy-geos) ;; not really an issue anymore
   :total-addresses (total-addresses db)
   :min-max-days (date-only-min-max db)
   :min-max-timestamps   (timestamps-min-max db)})

(comment

  (total-records @db/db)
  (spot-check @db/db)
  
  (types-total @db/db)
  (type-counts @db/db)

  (disposition-total @db/db)
  (disposition-counts @db/db)
  
  (description-total @db/db)

  (missing-important-stuff @db/db)

  (quick-status @db/db)

  (timestamps-min-max @db/db)

  (date-only-min-max @db/db)
  (days-total @db/db)
  
  (total-addresses @db/db)
  (address-counts @db/db)

  (geo-counts @db/db)

  (future
    (->> (most-recent-by-geo)
         time
         (urepl/massive-spew "/tmp/output.edn")))

  
  (botchy-geos @db/db)
  
  
  (urepl/massive-spew "/tmp/output.edn" *1)


  ;; DOH! but in geo functions is pushing the address into geo.
  ;; geocoding, like zen, is pain and suffering.
  (->> @db/db
       vals
       (map :geo)
       (filter #(= java.lang.String (type %)))
       (take 10))

  (utils/all-keys @db/db :type)
  (utils/all-keys @db/db :disposition)
  (utils/all-keys @db/db :geo)
  (utils/all-keys @db/db :address)
  
  (-> @db/db
      vals
      (utils/unnecessarily-complex-containsq :description "Canyon"))


  ;; check for non-numeric keys
  (->> @db/db
       keys
       (map type)
       frequencies)

  


  )


