(ns incidents.api
  (:require [liberator.core :as liberator]
            [incidents.db :as db]
            [incidents.utils :as utils]
            [taoensso.timbre :as log]
            [incidents.reports :as reports]
            [clojure.walk :as walk]
            [compojure.core :as compojure]
            [compojure.route :as route])
  (:import java.util.Date))


(defn serialize-for-json
  [t]
  (walk/postwalk #(if (= java.util.Date (class %))
                    (.getTime %)
                    %)
                 t))


(defn- with-count
  [{:keys [count]} xs]
  (cond->> xs count (take (Integer/parseInt count))))


(defn- with-dates
  [{:keys [min max]} xs]
  (if (and min max)
    (let [min (Long/parseLong min)
          max (Long/parseLong max)]
      (filter (fn [{:keys [time]}]
                (let [millis (.getTime time)]
                  (and (> millis min)
                       (< millis max)))) xs))
    xs))


(defn get-all
  [params]
  (->> @db/db
       vals
       (with-dates params)
       (sort-by :time)
       reverse
       (with-count params) ;; must be last before serializing
       serialize-for-json))

;; This is ugly, but if it needs to support sorting/searching
;; by dates, with a count, by lat-long eventually, etc,
;; this bolus will  be necessary
(defn get-geos
  [params]
  (->> (let [sorted (->> @db/db
                         vals
                         (with-dates params)
                         (sort-by :time)
                         reverse)]
         (for [geo  (utils/all-keys @db/db :geo)]
           (->> sorted
                (filter #(= geo (:geo %)))
                first)))
       (with-count params)
       serialize-for-json))



(liberator/defresource incidents
  :method-allowed? (liberator/request-method-in :get)
  
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :see-other (fn [context]
               (:new-url context))
  :handle-ok (fn [{{:keys [params]} :request}]
               (log/debug params)
               (get-all params)))



(liberator/defresource status
  :method-allowed? (liberator/request-method-in :get)
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :handle-ok (fn [{{:keys [params]} :request}]
               (reports/quick-status)))


(liberator/defresource min-max-timestamps
  :method-allowed? (liberator/request-method-in :get)
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :handle-ok (fn [{{:keys [params]} :request}]
               (reports/timestamps-min-max)))

(liberator/defresource geos
  :method-allowed? (liberator/request-method-in :get)
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :handle-ok (fn [{{:keys [params]} :request}]
               (get-geos params)))


(compojure/defroutes routes
  (compojure/ANY "/api" [] incidents)
  (compojure/ANY "/api/geos" [] geos)
  (compojure/ANY "/api/dates" [] min-max-timestamps)
  (compojure/ANY "/api/status" [] status))


(comment




  

  )
