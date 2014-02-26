(ns incidents.api-test
  (:import org.joda.time.DateTime)
  (:require [clojure.test :refer :all]
            [clj-http.client :as client]
            [incidents.utils :as utils]
            [incidents.db :as db]
            [incidents.server :as srv]
            [cheshire.core :as json]
            [incidents.reports :as reports]
            [utilza.repl :as urepl])
  (:use incidents.api))


(defn generate-random-sample-for-test-data
  "To prepare for functional tests, a subset of the db"
  []
  (into {} (repeatedly 200 #(rand-nth (vec @db/db)))))


(defn test-db
  []
  (-> "resources/testdata/testdb.edn"
      slurp
      clojure.edn/read-string))


(comment

  [1338366000000 1392018300000]

  ;; TODO: make these into unit tests!
  
  (get-all {:count "1"
            :min "1338366000000"
            :max "1392013560000"})


  (get-all {:count "1"})

  (->> (get-geos {})
       json/decode
       (urepl/massive-spew "/tmp/output.edn"))
  

  (map #(Double/parseDouble %) (vals {:lat "37.6539574", :lng "-122.4857181"}))
  
  (get-all {:lat "37.6539574", :lng "-122.4857181"})
  (get-all {:lat "37.6408391", :lng "-122.4903562"})
  
  ;; DOH!
  (for [i  (get-all {:count "10"})]
    (-> i
        :time
        DateTime.))

  (->> (srv/app {:uri "/api/status"
                 :db (test-db)
                 :request-method :get})
       :body
       json/decode)


  ;; (spit "/tmp/foo.js")
  
  (->> (srv/app {:uri "/api"
                 :db (test-db)
                 :request-method :get
                 :query-string "count=2"})
       :body
       json/decode)

  (->> (srv/app {:uri "/api/geos"
                 :db (test-db)
                 :request-method :get
                 :query-string "count=2"})
       :body
       json/decode)  



  (->> (srv/app {:uri "/api/geos"
                 :db (test-db)
                 :request-method :get})
       :body
       json/decode
       count)
  
  (->> (srv/app {:uri "/api"
                 :db (test-db)
                 :request-method :get
                 :query-string "count=5&min=1338366000000&max=1392013560000"})
       :body
       json/decode)

  
  (->> (srv/app {:uri "/api"
                 :db (test-db)
                 :request-method :get
                 :query-string "lat=37.6408391&lng=-122.4903562"})
       :body
       json/decode)
  
  (->> (srv/app {:uri "/api"
                 :db (test-db)
                 :request-method :get
                 :query-string "lat=37.6408391&lng=-122.4903562&min=1338366000000&max=1392013560000"})
       :body
       json/decode
       count)

  (->> (srv/app {:uri "/api/dates"
                 :db (test-db)
                 :request-method :get})
       :body
       json/decode)
  
  (->> (srv/app {:uri "/api/types"
                 :db (test-db)
                 :request-method :get})
       :body
       json/decode)

  (->> (srv/app {:uri "/api/types/stats"
                 :db (test-db)
                 :request-method :get})
       :body
       json/decode)
  
  (->> (srv/app {:uri "/api/dispositions"
                 :db (test-db)
                 :request-method :get})
       :body
       json/decode)


  (->> (srv/app {:uri "/api/dispositions/stats"
                 :db (test-db)
                 :request-method :get})
       :body
       json/decode)


  
  (->> (srv/app {:uri "/api"
                 :db (test-db)
                 :request-method :get
                 :query-string "search=Canyon"})
       :body
       json/decode)

  
  )

(comment
  ;; live
  (->> (client/get "http://incidents.bamfic.com/api/status" {:as :json})
       :body)

  
  (utils/all-keys @db/db :type)

  (-> "resources/testdata/testdb.edn"
      slurp
      clojure.edn/read-string
      (utils/all-keys :type))
  
  
  (count *1)
  

  (->> (srv/app {:uri "/api/types"
                 :db (test-db)
                 :request-method :get})
       :body
       json/decode)
  
  )
