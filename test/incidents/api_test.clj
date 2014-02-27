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


(defn get-uri-from-test-db
  "Fetches the URI from the API handler, using the query string supplied.
  Parses the returned JSON and returns keyworded EDN"
  [uri query-string]
  (->> (srv/app {:uri uri
                 :query-string query-string
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode))


(deftest status-test
  (testing "status")
  (is (= (get-uri-from-test-db "/api/status" "")
         {:total-incidents 199,
          :total-types 199,
          :total-dispositions 198,
          :total-descriptions 198,
          :total-geos 186,
          :total-addresses 186,
          :min-max-days {:max "2014-02-14",
                         :min "2012-06-03"},
          :min-max-timestamps {:max 1392406620000,
                               :min 1338760800000}})))
         

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
  (testing "all the unfiltered geos. should show only UNIQUE geos, with most recent incident for each")
  (is (= (-> "resources/testdata/geos-api-all.edn"
             slurp
             clojure.edn/read-string)
         (->> (srv/app {:uri "/api/geos"
                        :db (test-db)
                        :request-method :get})
              :body
              keyed-decode))))


(deftest incidents-by-date
  (testing "all the incidents within a date range")
  (is (= '({:geo {:lat 37.6511954, :lng -122.4841703},
            :address "Monterey Rd, Pacifica, CA",
            :time 1391496000000,
            :type "Dist Domestic",
            :id 140203252,
            :disposition "Report Taken.",
            :description
            "Occurred on Monterey Rd, Pacifica. Screaming in bcakgroung // x screaming for pd // bf is losing it no  weapons // unk drinking or drugs -- may be on drugs"}
           {:geo {:lat 37.63275369999999, :lng -122.492436},
            :address "Montecito Av, Pacifica, CA",
            :time 1391026260000,
            :type "Property Incident",
            :id 140129096,
            :disposition "Report Taken.",
            :description
            "Occurred on Montecito Av, Pacifica. Service Class: VOIP  subj outside wma  whi tshirt khaki pants not  suppose to be here  last seen on foot twds the beach. ."}
           {:geo {:lat 37.5905072, :lng -122.47599},
            :address "Terra Nova Bl. , Pacifica, CA",
            :time 1390796100000,
            :type "Civil Case",
            :id 140126222,
            :disposition "Assisted.",
            :description
            "Occurred on Terra Nova Bl. , Pacifica. Regarding custody issues"}
           {:geo {:lat 37.58244, :lng -122.48358},
            :address "Linda Mar Bl/Capistrano Dr, Pacifica, CA",
            :time 1390684740000,
            :type "Warrant Arrest",
            :id 140125130,
            :disposition "Arrest Made.",
            :description
            "Officer initiated activity at Linda Mar Bl/Capistrano Dr, Pacifica."}
           {:geo {:lat 37.5928217, :lng -122.4988326},
            :address "Arguello Bl, Pacifica, CA",
            :time 1390261380000,
            :type "Citizen Assist",
            :id 140120138,
            :disposition "Log Note Only.",
            :description
            "Occurred on Arguello Bl, Pacifica. Ticket sign off /rp not able to leave his home/"})
         (->> (srv/app {:uri "/api"
                        :db (test-db)
                        :request-method :get
                        :query-string "count=5&min=1338366000000&max=1392013560000"})
              :body
              keyed-decode))))


(deftest by-geos
  (testing "search by lat/lng")
  (is (= '({:geo {:lat 37.6408391, :lng -122.4903562},
            :address
            "Ocean Shore Elementary School on Oceana Bl. , Pacifica, CA",
            :time 1367445000000,
            :type "Susp Circ 911",
            :id 130501187,
            :disposition "Checks Ok.",
            :description
            "Occurred at Ocean Shore Elementary School on Oceana Bl. , Pacifica. 911 HANGUP"})
         (->> (srv/app {:uri "/api"
                        :db (test-db)
                        :request-method :get
                        :query-string "lat=37.6408391&lng=-122.4903562"})
              :body
              keyed-decode))))


#_(deftest by-geo-and-date
    (testing "by geo AND date")
    ;; TODO: find a better lat/lng and date combination in the subset db, that has > 1 record
    (->> (srv/app {:uri "/api"
                   :db (test-db)
                   :request-method :get
                   :query-string "lat=37.6408391&lng=-122.4903562&min=1338366000000&max=1392013560000"})
         :body
         keyed-decode))


(deftest min-max-dates
  (testing "min and max dates")
  (is (= {:max 1392406620000, :min 1338760800000}
         (->> (srv/app {:uri "/api/dates"
                        :db (test-db)
                        :request-method :get})
              :body
              keyed-decode))))

(deftest text-search
  (testing "text search")
  (is (= '({:geo {:lat 37.6509378, :lng -122.4772329},
            :address "Inverness Dr, Pacifica, CA",
            :time 1389787500000,
            :type "Police Mutual Aid",
            :id 140115018,
            :disposition "Report Taken.",
            :description
            "Officer initiated activity at Inverness Dr, Pacifica.1030 veh unoccupied  -- vehicle facing southbound. . "})
         (->> (srv/app {:uri "/api"
                        :db (test-db)
                        :request-method :get
                        :query-string "search=vehicle"})
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