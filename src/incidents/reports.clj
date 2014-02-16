(ns incidents.reports
  (:require [clojure.edn :as edn]
            [utilza.repl :as urepl]
            [incidents.db :as db]
            [environ.core :as env]))

(defn disposition-counts
  []
  (->> (for [d (->> @db/db
                    vals
                    (map :disposition)
                    set)]
         [d (count (filter (partial = d) (map :disposition (vals @db/db))))])
       (sort-by second)
       reverse))




(defn all-types
  []
  (->> @db/db
       vals
       (map :type)
       set))



(defn disposition-total-count
  []
  (->> @db/db
       vals
       (filter :disposition)
       count))


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

(defn description-contains
  [str]
  (->> @db/db
       (filter #(some-> % :description (.contains str)))
       (sort-by :time)))

