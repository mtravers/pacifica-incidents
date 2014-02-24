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
                       (< millis max))))
              xs))
    xs))

(defn- with-geo
  [{:keys [lat lng]} xs]
  (if (and lat lng)
    (let [chosen-lat (Double/parseDouble lat)
          chosen-lng (Double/parseDouble lng)]
      (filter (fn [{{:keys [lat lng]} :geo}]
                (and (= lat chosen-lat)
                     (= lng chosen-lng))) xs))
    xs))


(defn- with-types
  [{:keys [typess]} xs]
  ;; TODO: filter #{} set of types supplied
  )

(defn- with-search-string
  "filter results based on search string"
  [{:keys [search]} xs]
  ;; TODO: filter #(utils/simple-contains :description search)
  ;; TODO: will need to mod simple-contains to take the vals supplied
  )

(defn get-all
  [params]
  (->> @db/db
       vals
       (with-geo params)
       (with-dates params)
       ;;(with-search-string params)
       ;;(with-types params)
       (sort-by :time)
       reverse
       (with-count params) ;; must be last before serializing
       serialize-for-json))


;; Sorry this looks like ass, but it works.
;; Make things as simple as possible, but no simpler.
(defn get-geos
  "Gets only unique geos, summarized by date and count params."
  [params]
  (let [sorted (->> @db/db
                    vals
                    (with-dates params)
                    ;;(with-search-string params)
                    ;;(with-types params)
                    (sort-by :time)
                    reverse)
        geos (utils/all-keys @db/db :geo)]
    (->> (for [g geos]
           (->> sorted
                (filter #(= g (:geo %)))
                first))
         (filter map?) ;; skip the nil's and empties.
         (with-count params)
         serialize-for-json
         )))

(liberator/defresource incidents
  :method-allowed? (liberator/request-method-in :get)
  
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :see-other (fn [context]
               (:new-url context))
  :handle-ok (fn [{{:keys [params]} :request}]
               ;;(log/debug params) don't even need this.
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





(liberator/defresource all-dispositions
  :method-allowed? (liberator/request-method-in :get)
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :handle-ok (fn [{{:keys [params]} :request}]
               (->> :disposition
                    (utils/all-keys @db/db)
                    vec)))

(liberator/defresource disposition-stats
  :method-allowed? (liberator/request-method-in :get)
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :handle-ok (fn [{{:keys [params]} :request}]
               ;; TODO: filter these based on params, possibly by adding ->> with-date, with-geo
               ;; requires refactoring key-set-counts to take a seq of maps (->> @db/db vals), not a db
               (reports/disposition-counts)))

(liberator/defresource all-types
  :method-allowed? (liberator/request-method-in :get)
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :handle-ok (fn [{{:keys [params]} :request}]
               (->> :type
                    (utils/all-keys @db/db)
                    vec)))

(liberator/defresource type-stats
  :method-allowed? (liberator/request-method-in :get)
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :handle-ok (fn [{{:keys [params]} :request}]
               ;; TODO: filter these based on params, possibly by adding ->> with-date, with-geo
               ;; requires refactoring key-set-counts to take a seq of maps (->> @db/db vals), not a db
               (reports/type-counts)))

;; TODO: api endpoints for (reports/disposition-counts), (reports/type-counts), maybe (reports/address-counts)?
(compojure/defroutes routes
  (compojure/ANY "/api" [] incidents)
  (compojure/ANY "/api/geos" [] geos)
  (compojure/ANY "/api/dispositions" [] all-dispositions)
  (compojure/ANY "/api/dispositions/stats" [] disposition-stats)
  (compojure/ANY "/api/types" [] all-types)
  (compojure/ANY "/api/types/stats" [] type-stats)
  (compojure/ANY "/api/dates" [] min-max-timestamps)
  (compojure/ANY "/api/status" [] status))


(comment






  )
