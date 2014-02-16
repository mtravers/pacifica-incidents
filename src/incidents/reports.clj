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


(defn simple-contains
  [k str]
  (->> @db/db
       (filter #(some-> % k (.contains str)))
       (sort-by :time)))

;;;;;;


(defn disposition-total
  []
  (total-not-null-counts :disposition))

(defn disposition-counts
  []
  (key-set-counts :disposition))

(defn all-types
  []
  (total-not-null-counts :type))


(defn type-counts
  []
  (key-set-counts :type))


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

(comment

  (simple-contains :description "Canyon")

  (type-counts)

  (urepl/massive-spew "/tmp/output.edn" *1)
  
  )


