(ns incidents.dl
  (:import org.joda.time.DateTime)
  (:require [clj-http.client :as client]
            [utilza.java :as ujava]))

;; TODO: move to env
(def default-dl-url "http://www.pacificaindex.com/policelogs/PPDdailymediabulletin%s.pdf")

(defn fmt-date
  [d]
  (-> d
      .toYearMonthDay
      .toString))

(defn get-pdf
  "Takes datetime, and saves the fetched file to directory supplied. optional :url formatted url"
  [dt dir & {:keys [url]}]
  (let [dname (fmt-date dt)
        fname (format "%s/%s.pdf"  dir dname)]
    (->> dname
         (format (or url default-dl-url))
         (#(client/get % {:as :byte-array}))
         :body
         (ujava/spit-bytes fname))))




(comment
  ;; they seem to only go back to may 15 2012
  (reverse (ujava/date-range (DateTime. "2012-05-15") (DateTime.)))

  (get-pdf (DateTime. "2012-05-15") "/tmp")


  (doseq [d (reverse (ujava/date-range (DateTime. "2013-01-01") (DateTime.)))]
    (try
      (get-pdf d "/tmp")
      (catch Exception _))
    (Thread/sleep 2000))






  
  )



