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



(defn keyed-decode
  "Because I don't feel like messing around with as->"
  [s]
  (json/decode s true))



(deftest status-test
  (testing "status")
  (is (= {:total-incidents 199,
          :total-types 199,
          :total-dispositions 198,
          :total-descriptions 198,
          :total-geos 186,
          :total-addresses 186,
          :min-max-days {:max "2014-02-14",
                         :min "2012-06-03"},
          :min-max-timestamps {:max 1392406620000,
                               :min 1338760800000}}
         (->> (srv/app {:uri "/api/status"
                        :db (test-db)
                        :request-method :get})
              :body
              keyed-decode))))



(deftest count-apis
  (testing "first two apis")
  (is (= '({:geo {:lat 37.5876447, :lng -122.4682416},
            :address "Oddstad Bl. , Pacifica, CA",
            :time 1392406620000,
            :type "Fraud",
            :id 140214093,
            :disposition "Report Taken.",
            :description "Occurred on Oddstad Bl. , Pacifica. Rp at pd."}
           {:geo {:lat 37.6468476, :lng -122.487535},
            :address "Avalon Dr, Pacifica, CA",
            :time 1392338880000,
            :type "Suspicious Vehicle",
            :id 140213214,
            :disposition "Citation.",
            :description "Officer initiated activity at Avalon Dr, Pacifica. ."})
         (->> (srv/app {:uri "/api"
                        :db (test-db)
                        :request-method :get
                        :query-string "count=2"})
              :body
              keyed-decode))))



(deftest count-geos
  (testing "first two geos")
  (is (= '({:geo nil,
            :address nil,
            :time 1381024140000,
            :type "Fire Assist",
            :id 131005161,
            :disposition "Log Note Only.",
            :description "Officer initiated activity Cabrillo Hwy, Pacifica. ."}
           {:geo {:lat 37.6408391, :lng -122.4903562},
            :address
            "Ocean Shore Elementary School on Oceana Bl. , Pacifica, CA",
            :time 1367445000000,
            :type "Susp Circ 911",
            :id 130501187,
            :disposition "Checks Ok.",
            :description
            "Occurred at Ocean Shore Elementary School on Oceana Bl. , Pacifica. 911 HANGUP"})
         (->> (srv/app {:uri "/api/geos"
                        :db (test-db)
                        :request-method :get
                        :query-string "count=2"})
              :body
              keyed-decode))))



(deftest all-geos
  (testing "all the unfiltered geos. should show only UNIQUE geos, with most recent incident")
  (is (= (-> "resources/testdata/geos-api-all.edn"
             slurp
             clojure.edn/read-string)
         (->> (srv/app {:uri "/api/geos"
                        :db (test-db)
                        :request-method :get})
              :body
              keyed-decode))))


(comment

  [1338366000000 1392018300000]

  ;; TODO: make these into unit tests!
  
  (get-all {:count "1"
            :min "1338366000000"
            :max "1392013560000"})


  (get-all {:count "1"})

  (->> (get-geos {})
       keyed-decode
       (urepl/massive-spew "/tmp/output.edn"))
  

  (map #(Double/parseDouble %) (vals {:lat "37.6539574", :lng "-122.4857181"}))
  
  (get-all {:lat "37.6539574", :lng "-122.4857181"})
  (get-all {:lat "37.6408391", :lng "-122.4903562"})
  
  ;; DOH!
  (for [i  (get-all {:count "10"})]
    (-> i
        :time
        DateTime.))




  ;; (spit "/tmp/foo.js")
  

  
  (->> (srv/app {:uri "/api"
                 :db (test-db)
                 :request-method :get
                 :query-string "count=5&min=1338366000000&max=1392013560000"})
       :body
       keyed-decode)

  
  (->> (srv/app {:uri "/api"
                 :db (test-db)
                 :request-method :get
                 :query-string "lat=37.6408391&lng=-122.4903562"})
       :body
       keyed-decode)
  
  (->> (srv/app {:uri "/api"
                 :db (test-db)
                 :request-method :get
                 :query-string "lat=37.6408391&lng=-122.4903562&min=1338366000000&max=1392013560000"})
       :body
       keyed-decode
       count)

  (->> (srv/app {:uri "/api/dates"
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode)
  
  (->> (srv/app {:uri "/api/types"
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode)

  (->> (srv/app {:uri "/api/types/stats"
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode)
  
  (->> (srv/app {:uri "/api/dispositions"
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode)


  (->> (srv/app {:uri "/api/dispositions/stats"
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode)


  
  (->> (srv/app {:uri "/api"
                 :db (test-db)
                 :request-method :get
                 :query-string "search=Canyon"})
       :body
       keyed-decode)

  
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
       keyed-decode)
  
  )


(comment

  (run-tests)

  )