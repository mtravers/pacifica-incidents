(ns incidents.reports
  (:import org.joda.time.DateTime)
  (:require [clojure.edn :as edn]
            [utilza.repl :as urepl]
            [incidents.db :as db]
            [incidents.utils :as utils]
            [incidents.geo :as geo]
            [clojure.core.reducers :as r]
            [environ.core :as env]))




(defn disposition-total
  []
  (utils/total-not-null-counts :disposition))

(defn disposition-counts
  []
  (utils/key-set-counts :disposition))

(defn types-total
  []
  (utils/total-not-null-counts :type))


(defn type-counts
  []
  (utils/key-set-counts :type))

(defn geo-total
  []
  (utils/total-not-null-counts :geo))

(defn description-total
  []
  (utils/total-not-null-counts :description))

(defn total-records
  []
  (->> @db/db
       vals
       count))



(defn spot-check
  []
  (get @db/db (-> @db/db
                  keys
                  rand-nth)))


(defn missing-important-stuff
  []
  (let [total (total-records)]
    (reduce (fn [a k]
              (assoc a k  (- total (utils/key-set-counts k))))
            {}
            [:description :type :disposition])))


(defn unique-dates
  "Unique dates without timestamps in the db"
  []
  (reduce #(conj %1 %2)
          #{}
          (r/map #(-> %
                      :time
                      org.joda.time.DateTime.
                      .toLocalDate
                      .toString)
                 (->> @db/db
                      vals))))

(defn date-only-min-max
  "poorly named"
  []
  (->> (unique-dates)
       sort
       ((juxt first last))
       (zipmap [:min :max])))

(defn days-total
  []
  (count (unique-dates)))

(defn timestamps-min-max
  []
  (->> @db/db
       vals
       (map :time)
       sort
       (map #(.getTime %))
       ((juxt first last))
       (zipmap [:min :max])))



(defn  address-counts
  []
  (utils/key-set-counts :address))

(defn total-addresses
  []
  (utils/total-not-null-counts :address))

(defn botchy-geos
  []
  (for [[k v] (utils/key-set-counts (comp type :geo))]
    [(str k) v]))



(defn quick-status
  []
  {:total-incidents (total-records)
   :total-types (types-total)
   :total-dispositions (disposition-total)
   :total-descriptions (description-total)
   :total-geos (geo-total)
   ;; :botchy-geos (botchy-geos) ;; not really an issue anymore
   :total-addresses (total-addresses)
   :min-max-days (date-only-min-max)
   :min-max-timestamps   (timestamps-min-max)})

(comment

  (total-records)
  (spot-check)
  
  (types-total)
  (type-counts)

  (disposition-total)
  (disposition-counts)
  
  (description-total)

  (missing-important-stuff)

  (quick-status)

  (timestamps-min-max)

  (date-only-min-max)
  (days-total)
  
  (total-addresses)
  (future
    (->> (address-counts)
         (urepl/massive-spew "/tmp/output.edn")))

  
  (botchy-geos)
  
  
  (urepl/massive-spew "/tmp/output.edn" *1)


  ;; DOH! but in geo functions is pushing the address into geo.
  ;; geocoding, like zen, is pain and suffering.
  (->> @db/db
       vals
       (map :geo)
       (filter #(= java.lang.String (type %)))
       (take 10))

  (utils/all-keys @db/db :type)

  (utils/simple-contains :description "Canyon")


  ;; check for non-numeric keys
  (->> @db/db
       keys
       (map type)
       frequencies)

  





  
  )


