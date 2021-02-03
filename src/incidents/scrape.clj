(ns incidents.scrape
  (:require [incidents.db :as db]
            [clj-http.client :as client]
            [environ.core :as env]
            [incidents.reports :as reports]
            [incidents.geo :as geo]
            [net.cgrand.enlive-html :as enlive]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as log])
  (:gen-class))

;; logs all errors and continues on its merry way.
(defonce dl-agent (agent nil :error-handler #(log/error %)))

;;; TODO use fs
(defn basepath
  [path]
  (second (re-matches #"(.+?://.*?)/.*" path)))

(defn get-url [url]
 (-> url
     (client/get {:insecure? true})
     :body))

(defn get-url-as-stream [url]
 (-> url
     (client/get {:insecure? true :as :stream})
     :body))

(defn scrape-urls
  [s]
  (let [base (basepath s)
        links (-> s
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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment





  (get-all-pdfs! @db/db  "http://www.cityofpacifica.org/depts/police/media/media_bulletin.asp")
  (-main))

 
(comment
(def index-url "https://www.cityofpacifica.org/depts/police/media/media_bulletin.asp")

(def files
  (->> index-url
       scrape-urls
       (map download)
       ))
