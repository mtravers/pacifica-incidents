(ns incidents.reports
  (:require [clojure.edn :as edn]
            [utilza.repl :as urepl]
            [incidents.db :as db]
            [environ.core :as env]))


(defn key-set-counts
  [k]
  (->> (for [d (->> @db/db
                    vals
                    (map k)
                    set)]
         [d (count (filter (partial = d) (map k (vals @db/db))))])
       (sort-by second)
       reverse))

(defn total-not-null-counts
  [k]
  (->> @db/db
       vals
       (filter k)
       count))

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
  (total-not-null-counts :disposition))

(defn disposition-counts
  []
  (key-set-counts :disposition))

(defn types-total
  []
  (total-not-null-counts :type))


(defn type-counts
  []
  (key-set-counts :type))


(defn description-total
  []
  (total-not-null-counts :description))

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
              (assoc a k  (- total (key-set-counts k))))
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

(defn quick-status
  []
  {:total-incidents (total-records)
   :total-types (types-total)
   :total-dispositions (disposition-total)
   :total-descriptions (description-total)
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
  
  (urepl/massive-spew "/tmp/output.edn" *1)
  
  )


