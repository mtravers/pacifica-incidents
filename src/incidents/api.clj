(ns incidents.api
  (:require [incidents.db :as db]
            [incidents.utils :as utils]
            [ring.util.response :as rutil]
            [markdown.core :as md]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [incidents.reports :as reports]
            [compojure.core :as compojure]
            [org.parkerici.multitool.core :as u]
            [clojure.string :as str]
            )
  (:import java.util.Date))


(defn json-response
  "Takes some clojure data, encodes it as JSON, wraps it in a ring response,
  adds the right header, and returns it."
  [data]
  {:status 200
   :headers {"Content-Type" "application/json;charset=UTF-8"
             ;; TODO: yeah, OK, maybe ETags might be useful in the future.
             "Pragma" "no-cache"
             "Expires" "Wed, 11 Jan 1984 05:00:00 GMT"
             "Cache-Control" "private, no-cache, no-store"}
   :body (json/encode data true)})


;;; Ensure dates get written out as ints
(extend java.util.Date
  clojure.data.json/JSONWriter
  {:-write (fn [x out]
             (.print out (.getTime x)))})

(defn tweak-for-json
  [incidents]
  (->> incidents
       (filter :geo)
       (filter :id)                     ;TODO temp, should give these a synthetic id
       (map #(select-keys % [:id :datime :geo :type :location :disposition]))))

;;; Don't understand what this is for
(defn- with-count
  [{:keys [count]} xs]
  (cond->> xs count (take (Integer/parseInt count))))

(defn- with-dates
  [{:keys [min max] :as _params} xs]
  (if (and min max)
    (filter (fn [{:keys [datime]}]
              (u/<* min datime max))
            xs)
    xs))

(defn- convert-geos
  "Takes a params map, and returns a map of :chosen-lat and :chosen-lng IFF the input is valid"
  [{:keys [lat lng]}]
  (when (and lat lng (every? #(-> % empty? not) [lat lng]))
    (let [lat (Double/parseDouble lat)
          lng (Double/parseDouble lng)]
      (when (every? (partial not= 0.0 ) [lat lng])
        {:chosen-lat lat
         :chosen-lng lng}))))


(defn- with-geo
  "Filter results based on lat lng geo ranges"
  [params xs]
  (if-let [{:keys [chosen-lat chosen-lng]} (convert-geos params)]
    (filter (fn [{{:keys [lat lng]} :geo}]
              (and (= lat chosen-lat)
                   (= lng chosen-lng)))
            xs)
    xs))

(defn- with-search-string
  "filter results based on search string"
  [{:keys [search]} xs]
  (if search
    (filter (fn [entry]
              (some (fn [key]
                      (and (key entry)
                           (str/includes? (key entry) search)))
                    [:type :disposition :location]))
            xs)
    xs))

(defn- coerce-date
  [s]
  (cond
    (string? s) (Date. (Long/parseLong s))
    (number? s) (Date. s)
    :else nil))

(defn- coerce-params
  [params]
  (-> params
      (update :min coerce-date)
      (update :max coerce-date)))


(defn get-all
  [params]
  (let [params (coerce-params params)]
    (->> (db/entries)
         (with-geo params)
         (with-dates params)
         (with-search-string params)
                                        ;       (sort-by :time)
                                        ;       reverse
                                        ;       (with-count params) ;; must be last before serializing
         tweak-for-json
         )))


;; Sorry this looks like ass, but it works.
;; Make things as simple as possible, but no simpler.
(defn get-geos
  "Gets only unique geos, summarized by date and count params."
  [db params]
  (let [params (coerce-params params)]
    (->> (db/entries)
         (with-dates params)
         (with-search-string params)
         ;; (with-types params)
         ;; (sort-by :datime)
         ;; reverse
         tweak-for-json
         )))

;;; This did some kind of grouping by geo? 
    ;;     geos (utils/all-keys db :geo)]
    ;; (->> (for [g geos]
    ;;        (->> sorted
    ;;             (filter #(= g (:geo %)))
    ;;             first)) ;; only want the most recent.
    ;;      (filter map?) ;; skip the nil's and empties.
    ;;      (with-count params)



(compojure/defroutes routes

  ;; Get all incidents matching 
  (compojure/GET "/" {:keys [params]}
                 (-> (get-all params)
                     json-response))
  
  ;; No longer used
  (compojure/GET "/dates"  {:keys [db]}
                 (-> (or db @db/db)
                     reports/timestamps-min-max
                     json-response))

  ;; Not currently using, we display individual incidents not grouped by geo
  (compojure/GET "/geos" {:keys [params db]}
                 (-> (or db @db/db)
                     (get-geos params)
                     json-response))
  
  ;; Following are not used in code, for devops I guess?

  ;; TODO: error handling/validation for non-valid keys?
  (compojure/GET "/keys/:kind" {{:keys [kind]} :params db :db}
                 (some->> kind
                          keyword
                          (utils/all-keys (or db @db/db))
                          vec
                          json-response))

  

  ;; TODO: error handling/validation for non-valid keys?
  (compojure/GET "/stats/:kind" {{:keys [kind]} :params db :db}  
                 (some->> kind
                          keyword 
                          (utils/key-set-counts (or db @db/db))
                          json-response))


  
  ;; should really be a PUT or something, but whatever.
  ;; XXX the function taht this endpoint calls has mysteriously vanished
  #_(compojure/GET "/scrape" {:keys [db]}
                 (-> (or db @db/db)
                     scrape/start-pdf-downloading
                     (pr-str "running")
                     json-response))

  (compojure/GET "/docs" {:keys [_params]}
                 (-> "doc/API.md"
                     slurp
                     md/md-to-html-string))
  
  (compojure/GET "/status" {:keys [db]}
                 (-> (or db @db/db)
                     reports/quick-status
                     json-response)))




