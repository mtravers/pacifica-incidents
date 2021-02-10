(ns incidents.utils
  (:require [incidents.db :as db]
            [clojure.core.reducers :as r]))


;;; Apply k to all vals of db. Not sure why it is named all-keys.
(defn all-keys
  [db k]
  (map k (vals db)))

(defn key-set-counts
  [db k]
  (->> db
       vals
       (map k)
       frequencies
       (sort-by second)
       reverse))

(defn total-not-null-counts
  [db k]
  (reduce (fn [c _] (inc c)) 0 (r/filter k (vals db))))


(defn simpler-contains [k s m]
  (some-> m k (.contains s)))


(defn unnecessarily-complex-contains
  "Takes vals seq so it can be threaded through with other searches/filters.
    vals is a seq of maps. k is the key to search for in those maps.
    s is the string to search for."
  [vals k s]
  ;; Took out the reducers, unnecessarily fancy.
  (filter #(some-> % k (.contains s)) vals))







