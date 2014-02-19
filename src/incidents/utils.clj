(ns incidents.utils
  (:require [incidents.db :as db]
            [clojure.core.reducers :as r]))


(defn all-keys
  [db k]
  (reduce #(conj %1 %2)
          #{}
          (r/map k (-> db vals))))

(defn key-set-counts
  [db k]
  (->> db
       vals
       (map k)
       frequencies
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