(ns incidents.scrape
  (:require [incidents.db :as db]
            [clj-http.client :as client]
            [environ.core :as env]
            [incidents.reports :as reports]
            [incidents.geo :as geo]
            [incidents.parse :as parse]
            [net.cgrand.enlive-html :as enlive]
            [taoensso.timbre :as log]))

(defonce dl-agent (agent nil))

(defn scrape-urls
  [s]
  (let [re #".*PPDdailymediabulletin.*?(\d+-\d+-\d+).*?pdf"]
    (for [a  (-> s
                 enlive/html-snippet
                 (enlive/select [:a]))
          :let [href (-> a :attrs :href)]
          :when (re-find re href) ]
      (let [[url date] (re-matches re href)]
        ;; TODO: make sure these urls are either absolute or add a host to them!!
        {:url url
         :date date}))))


(defn filter-not-in-db
  "Gets only those dates not yet in the db"
  [urlmaps]
  (let [in-db (reports/unique-dates)]
    (filter #(in-db %) urlmaps)))


(defn add-items-to-db!
  "Takes a list of items from a pdf scrape, and adds them to the db
   TODO: this too belongs somewhere else I bet"
  [items]
  (doseq [{:keys [id] :as item} items]
    (swap! db/db (fn [db]
                   (assoc db id (geo/add-geo-and-address item))))))


(defn fetch-and-add-to-db!
  "TODO: this might belong in another ns?"
  [url]
  (try
    (log/info "fetching " url)
    (-> url
        parse/pdf-to-text
        parse/parse-pdf-text
        add-items-to-db!)
    (catch Exception e
      (log/error e))))


;; XXX not really tested thoroughly yet.
(defn get-all-pdfs!
  "Takes an URL to the index page.
    Fetches and parses all the pdfs not in the db yet"
  [index-url]
  (log/info "fetching index from " index-url)
  (doseq [{:keys [date pdf-url]}  (->> index-url
                                       slurp
                                       scrape-urls
                                       filter-not-in-db)]
    (fetch-and-add-to-db! pdf-url)))


(defn start-pdf-downloading
  []
  (send-off dl-agent (fn [_]
                       (-> env/env
                           :dl-index-url
                           get-all-pdfs! ))))

(comment
  
  (get-all-pdfs! "/mnt/sdcard/tmp/logs/policelogs.html")
  
  (start-pdf-downloading)
  
  )