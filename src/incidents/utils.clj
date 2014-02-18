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
         [d (reduce (fn [c _] (inc c))  0 (r/filter (partial = d) (r/map k (vals @db/db))))])
       (sort-by second)
       reverse))

(defn total-not-null-counts
  [k]
  (reduce (fn [c _] (inc c)) 0 (r/filter k (vals @db/db))))


(defn simple-contains
  [k s]
  (->> (reduce #(conj %1 %2)
               []
               (->> @db/db
                    vals
                    (r/filter #(some-> % k (.contains s)))))
       (sort-by :time)))