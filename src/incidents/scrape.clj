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
  (-main)


(def index-url "https://www.cityofpacifica.org/depts/police/media/media_bulletin.asp")

(def files
  (->> index-url
       scrape-urls
       (map download)
       ))


(doseq [f files2]
  (println (format "aws textract start-document-analysis --document-location '{\"S3Object\":{\"Bucket\":\"incidents\",\"Name\":\"%s\"}}' --feature-types '[\"TABLES\"]'  --region us-west-2" (fs/base-name (:local f)))))

(def job-ids
  ["e04fee980c7b9fd3e20c3ec7e19c881818d5300be27226111bb81373a91dd85f"
  "b997a7ddc8dd7b79420bfd5c895e65e975db8a9d54f28d8835fa36a033b2a066"
  "c829b129970632eddeef0efa5e4bef4fd6b1f6dbd58dcbc8a5769ac5d9c3b774"
  "325947c6f246ff1415af663f915aba8d31890b48a0477b487f8f88319e148e91"
  "b2cca4356d704a631b77240dd897d8c7bcc877a199f6028431aeae9236b35073"
  "d69b7fbd27771b60cd2856ecee2d0cf920d1d90a5800d83d1247ff60db437cce"
  "5680594291c39cbd1c68bcdcefa6c7f40cbe189c87cc1ecd9fc6da019617a60e"
  "740c66b6cd1480b0e24d46d097565ea101a87bb54a5ae9694db625c77d2e077f"
  "5cee3228eef4974b746c1ef2aab505d9168ea996359df7281546a988c2e5ad80"
  "bab37866c214e188208b8695e9b3bd978bca33f551f589e859046fa35c212ddf"])

#_
(u/doseq* [f files2 job-id job-ids]
  (println
   (format
    "aws textract  get-document-analysis --job-id %s  --region us-west-2 > %s" job-id (str "data/parsed/" (fs/base-name (:local f))))))

  (def index-url "https://www.cityofpacifica.org/depts/police/media/media_bulletin.asp")

  (def files
    (->> index-url
         scrape-urls
         (map download)
         ))

  )

;;; Get textract output from job-ids
(u/doseq* [f (range (count job-ids)) job-id job-ids]
          (ju/schppit (str "data/parsed/" f (str ".json"))
                       (aws/job-id->blocks job-id)))




  (println
   (format
    "aws textract  get-document-analysis --job-id %s  --region us-west-2 > %s" job-id (str "data/parsed/" (fs/base-name (:local f))))))
