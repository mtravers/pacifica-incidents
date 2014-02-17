(ns incidents.utils
  (:require [incidents.db :as db]))


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

