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

(defn fix-malformed-url
  "Really? Pacificaindex has urls that are totally screwed up."
  [url]
  (.replace url "http:/www" "http://www"))

(defn scrape-urls
  "Takes a string with a parsable HTML page in it,
   and returns a seq of maps of urls and dates."
  [s]
  (for [a  (-> s
               enlive/html-snippet
               (enlive/select [:a]))]
    (when-let [[url date] (some->> a :attrs :href
                                   (re-matches #".*PPDdailymediabulletin.*?(\d+-\d+-\d+).*?pdf" ))]
      ;; TODO: make sure these urls are either absolute or add a host to them!!
      {:url (fix-malformed-url url)
       :date date})))


(defn filter-not-in-db
  "Gets only those dates not yet in the db"
  [urlmaps]
  (let [in-db (reports/unique-dates)]
    (->> urlmaps
         (remove #(in-db (some-> % :date)))
         (remove nil?))))


(defn add-items-to-db!
  "Takes a list of items from a pdf scrape, and adds them to the db
   TODO: this too belongs somewhere else I bet"
  [items]
  (doseq [{:keys [id] :as item} items]
    (swap! db/db (fn [db]
                   (assoc db id (geo/add-geo-and-address item))))))


(defn fetch-and-add-to-db!
  "Required to be a separate function for migration purposes."
  [url]
  (log/info "fetching " url)
  (-> url
      parse/pdf-to-text
      parse/parse-pdf-text
      add-items-to-db!))


(defn get-all-pdfs!
  "Takes an URL to the index page.
    Fetches and parses all the pdfs not in the db yet"
  [index-url]
  (log/info "fetching index from " index-url)
  (doseq [{:keys [date url]}  (->> index-url
                                   slurp
                                   scrape-urls
                                   filter-not-in-db)]
    (try
      (fetch-and-add-to-db! url)
      (catch Exception e
        (log/error e)))))


(defn start-pdf-downloading
  []
  (send-off dl-agent (fn [_]
                       (-> env/env
                           :dl-index-url
                           get-all-pdfs! ))))

(comment

  (-> "/mnt/sdcard/tmp/logs/policelogs.html"
      slurp
      scrape-urls)

  (-> (:dl-index-url env/env)
      slurp
      scrape-urls
      filter-not-in-db)


  ;; (urepl/massive-spew "/tmp/output.edn" *1)
  
  (get-all-pdfs! "/mnt/sdcard/tmp/logs/policelogs.html")

  (log/info "wtf?")
  
  (start-pdf-downloading)
  
  )