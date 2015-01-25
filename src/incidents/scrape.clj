(ns incidents.scrape
  (:require [incidents.db :as db]
            [clj-http.client :as client]
            [environ.core :as env]
            [incidents.reports :as reports]
            [incidents.geo :as geo]
            [incidents.parse :as parse]
            [net.cgrand.enlive-html :as enlive]
            [taoensso.timbre :as log]))

;; logs all errors and continues on its merry way.
(defonce dl-agent (agent nil :error-handler #(log/error %)))


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
      {:url url
       :date date})))


(defn filter-not-in-db
  "Gets only those dates not yet in the db"
  [db urlmaps]
  (let [in-db (reports/unique-dates db)]
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
  (try
    (log/info "fetching " url)
    (-> url
        parse/pdf-to-text
        parse/parse-pdf-text
        add-items-to-db!)
    ;; TODO: use dire to move this try/catch out of the program flow
    (catch Exception e
      (log/error e))))


(defn get-all-pdfs!
  "Takes an URL to the index page.
    Fetches and parses all the pdfs not in the db yet"
  [db index-url]
  (try
    (log/info "fetching index from " index-url)
    (doseq [{:keys [date url]}  (->> index-url
                                     slurp
                                     scrape-urls
                                     (filter-not-in-db db))]
      (fetch-and-add-to-db! url))
    ;; TODO: use dire to move this try/catch out of the program flow
    (catch Exception e
      (log/error e))
    (finally
      ;; Whatever got downloaded thus far, save it.
      (db/save-data!))))



(defn start-pdf-downloading
  [db]
  (send-off dl-agent (fn [_]
                       (->> env/env
                            :dl-index-url
                            (get-all-pdfs! db)))))

(defn -main []
  (db/db-init)
  (start-pdf-downloading @db/db)
  )

(comment

  (-> "/mnt/sdcard/tmp/logs/policelogs.html"
      slurp
      scrape-urls)

  (-> (:dl-index-url env/env)
      slurp
      scrape-urls
      (filter-not-in-db @db/db))


  ;; (urepl/massive-spew "/tmp/output.edn" *1)

  (get-all-pdfs! "/mnt/sdcard/tmp/logs/policelogs.html")

  (log/info "wtf?")

  (start-pdf-downloading @db/db)


  ;; TODO: somewhere in here, put a blacklist of known bad URLs (empty files)
  ;; probably best to have it in env/env, or maybe in metadata in the database?
  ;; e.g. known bads are  http://www.pacificaindex.com/pacificadocumentwire/4883-PPDdailymediabulletin(2012-05-15).pdf and
  ;;  http://www.pacificaindex.com/pacificadocumentwire/4994-PPDdailymediabulletin(2012-07-14).pdf


  (->> env/env
       :dl-index-url
       slurp
       scrape-urls)


  )
