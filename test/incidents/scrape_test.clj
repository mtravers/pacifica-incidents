(ns incidents.scrape-test
  (:import org.joda.time.DateTime
           java.io.File)
  (:require [clojure.test :refer :all]
            [instaparse.core :as ip]
            [utilza.repl :as urepl]
            [taoensso.timbre :as log]
            [incidents.scrape :refer :all]))

(comment


  (-> (:dl-index-url env/env)
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


  )

(comment

  (require '[utilza.repl :as urepl])

  (scrape-urls "http://www.cityofpacifica.org/depts/police/media/media_bulletin.asp")

  (index->pdfurls "/mnt/sdcard/tmp/media_bulletin.asp")

  (index->pdfurls "http://www.cityofpacifica.org/depts/police/media/media_bulletin.asp")
  
  (urepl/massive-spew "/tmp/foo.edn" *1)


  (url->filename  "http://cityofpacifica.org/civica/filebank/blobdload.asp?BlobID=2470")
  

  (filename->date "inline; filename=\"3-7-15 Media Bulletin.pdf\"")

  (basepath "http://www.cityofpacifica.org/depts/police/media/media_bulletin.asp")

  (log/set-level! :trace)

  (str "http://www.cityofpacifica.org" "/civica/filebank/blobdload.asp?BlobID=7314")

  (url->filename (str "http://www.cityofpacifica.org" "/civica/filebank/blobdload.asp?BlobID=7332"))

  (filename->date (url->filename (str "http://www.cityofpacifica.org" "/civica/filebank/blobdload.asp?BlobID=7332")))

  (->  "http://www.cityofpacifica.org/civica/filebank/blobdload.asp?BlobID=7314"
       parse/pdf-to-text
       parse/parse-pdf-text)
  
  )


