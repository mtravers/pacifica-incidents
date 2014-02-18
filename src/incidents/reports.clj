(ns incidents.reports
  (:require [clojure.edn :as edn]
            [utilza.repl :as urepl]
            [incidents.db :as db]
            [incidents.utils :as utils]
            [incidents.geo :as geo]
            [environ.core :as env]))


;; XXX borken, must fix
(defn simple-contains
  [k re]
  (->> @db/db
       vals
       (filter #(some->> % k (re-matches re)))
       (sort-by :time)))

;;;;;;


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


(defn dates-min-max
  []
  (->> @db/db
       vals
       (map :time)
       sort
       (map #(.getTime %))
       ((juxt first last))))



(defn  address-counts
  []
  (utils/key-set-counts :address))

(defn total-addresses
  []
  (utils/total-not-null-counts :address))

(defn botchy-geos
  []
  (utils/key-set-counts (comp type :geo)))


(defn quick-status
  []
  {:total-incidents (total-records)
   :total-types (types-total)
   :total-dispositions (disposition-total)
   :total-descriptions (description-total)
   :total-geos (geo-total)
   :botchy-geos (botchy-geos)
   :total-addresses (total-addresses)
   :min-max-dates   (dates-min-max)})

(comment

  (simple-contains :description #"Canyon")

  (total-records)
  (spot-check)
  
  (types-total)
  (type-counts)

  (disposition-total)
  (disposition-counts)
  
  (description-total)

  (missing-important-stuff)

  (quick-status)

  (dates-min-max)

  (total-addresses)
  (address-counts)

  ;; unique ones.
  (->> (address-counts)
       (map first)
       set
       count)
  
  (future
    (->> (address-counts)
         (urepl/massive-spew "/tmp/output.edn")))
  
  (urepl/massive-spew "/tmp/output.edn" *1)


  ;; DOH! but in geo functions is pushing the address into geo.
  ;; geocoding, like zen, is pain and suffering.
  (->> @db/db
       vals
       (map :geo)
       (filter #(= java.lang.String (type %)))
       (take 10))
  
  )


