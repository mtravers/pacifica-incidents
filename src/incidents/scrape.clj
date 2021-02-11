(ns incidents.scrape
  (:require [incidents.db :as db]
            [clj-http.client :as client]
            [environ.core :as env]
            [incidents.reports :as reports]
            [incidents.geo :as geo]
            [incidents.aws :as aws]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [me.raynes.fs :as fs]
            [org.parkerici.multitool.core :as u]
            [org.parkerici.multitool.cljcore :as ju]
            [taoensso.timbre :as log])
  (:gen-class))

(def index-url "https://www.cityofpacifica.org/depts/police/media/media_bulletin.asp")

(defn basepath
  [path]
  (second (re-matches #"(.+?://.*?)/.*" path)))

(defn get-url [url]
  (log/info "Retrieving" url)
  (-> url
      (client/get {:insecure? true})
      :body))

(defn get-url-as-stream [url]
  (log/info "Retrieving" url)
  (-> url
      (client/get {:insecure? true :as :stream})
      :body))

(defn scrape-urls
  "Returns seq of file descriptors with :url and :date fields"
  [page-url]
  (let [base (basepath page-url)
        links (-> page-url
                  get-url
                  enlive/html-snippet
                  (enlive/select [:a]))]
    (filter
     identity
     (map (fn [{:keys [attrs content]}]
            (let [date-string (and (string? (first content))
                                   (re-matches #"(.*) Media Bulletin" (first content)))]
              (when date-string
                {:url (str base (:href attrs))
                 :date (second date-string)}))) ;TODO parse date 
          links))))

(def pdf-directory "data/pdfs/")

;;; TODO maybe check to see if its downloaded already
(defn download [fmap]
  (let [date (s/replace (:date fmap) \/ \-)
        output-file (fs/file (str pdf-directory date ".pdf"))]
    (io/copy (get-url-as-stream (:url fmap))
             output-file)
    (assoc fmap :local (str output-file))))

(defn upload [fmap]
  (let [s3-key (str "/pdfs/" (fs/base-name (:local fmap)))]
    (aws/file->s3 (:local fmap) s3-key)
    (assoc fmap :s3 s3-key)))
      
(defn full-scrape!
  []
  (let [files (->> index-url
                   scrape-urls
                   (map download)
                   (map upload))]
    (db/update! assoc :files files)))

(defn incremental-scrape!
  []
  (let [db-files (u/index-by :date (:files @db/db))
        site-files (u/index-by :date (scrape-urls index-url))
        new (apply dissoc site-files (keys db-files))
        new-download (map (comp upload download) (vals new))]
    (db/update-in! [:files] conj new-download))) ;?

;;; For testing – remove some files from the db
(defn delete-files
  [n]
  (db/update-in! [:files] (partial drop n)))

