(ns incidents.utils
  (:require [incidents.db :as db]
            [clojure.core.reducers :as r]))


(defn all-keys
  [db k]
  (reduce #(conj %1 %2)
          #{}
          (r/map k (-> db vals))))

(defn key-set-counts
  [k]
  (->> (for [d (all-keys @db/db k)]
         [d (count (reduce #(conj %1 %2)  [] (r/filter (partial = d) (r/map k (vals @db/db)))))])
       (sort-by second)
       reverse))

(defn total-not-null-counts
  [k]
  (->> @db/db
       vals
       (filter k)
       count))

