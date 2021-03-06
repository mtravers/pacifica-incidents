(ns incidents.scrape
  (:require [incidents.db :as db]
            [clj-http.client :as client]
            [environ.core :as env]
            [incidents.reports :as reports]
            [incidents.geo :as geo]
            [incidents.parse :as parse]
            [net.cgrand.enlive-html :as enlive]
            [taoensso.timbre :as log])
  (:gen-class))

;; logs all errors and continues on its merry way.
(defonce dl-agent (agent nil :error-handler #(log/error %)))

(defn basepath
  [path]
  (second (re-matches #"(.+?://.*?)/.*" path)))

(defn index->pdfurls
  [s]
  (filter identity (for [a  (-> s
                                (client/get {:insecure? true})
                                :body
                                enlive/html-snippet
                                (enlive/select [:a]))]
                     (let [href (-> a
                                    :attrs
                                    :href)]
                       (when (and href (.contains href "BlobID"))
                         href)))))


(defn url->filename
  [s]
  (->  s   
       (client/head {:insecure? true})
       :headers
       (get "content-disposition")))

(defn filename->date
  [s]
  (let [fmt "%04d-%02d-%02d"
        [y d m] (for [n (-> (re-matches #"inline;.*?filename=\"(\d+)-(\d+)-(\d+).*?[Bulletin|MB]\.pdf\"" s)
                            rest
                            reverse)]
                  (Integer/parseInt n))]
    (log/trace (format fmt y m d))
    (format fmt (if (> 2000 y) (+ 2000 y) y) m d)))



(defn scrape-urls
  "Takes a url with an index page.
   and returns a seq of maps of urls and dates."
  [idx]
  (let [bn (basepath idx)]
    (log/info (format "basepath %s of url %s" bn idx))
    (for [url (index->pdfurls idx)]
      (try
        (let [full-url (str bn url)]
          (log/trace full-url)
          {:url full-url
           :date  (filename->date (url->filename full-url))})
        (catch Exception e
          (log/error e)
          nil)))))




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
    (doseq [{:keys [url]}  (->> index-url
                                scrape-urls
                                (filter identity)
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
  (let [{:keys [dl-index-url]} env/env]
    (when-not dl-index-url
      (throw (ex-info "You did not specify dl-index-url in env" env/env)))
    (send-off dl-agent (fn [_]
                         (get-all-pdfs! db dl-index-url)))))

(defn -main []
  (db/db-init)
  (start-pdf-downloading @db/db))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  (index->pdfurls "http://www.cityofpacifica.org/depts/police/media/media_bulletin.asp")

 
  (log/set-level! :trace)
  (-main)

 
  
  )
