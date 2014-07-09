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
  "Returns a map of a predictable, version-controlled
    subset of the database, for testing."
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
  (-> (srv/app {:uri uri
                :query-string query-string
                :db (test-db)
                :request-method :get})
      :body
      (json/decode true)))


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
  (let [first-2-incidents '({:geo {:lat 37.5876447, :lng -122.4682416},
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
                             :description "Officer initiated activity at Avalon Dr, Pacifica. ."})]
    (is (= (get-uri-from-test-db "/api" "count=2")
           first-2-incidents))
    (testing "ignore broken min/max strings")
    (is (= (get-uri-from-test-db "/api" "count=2&min=0&max=0")
           first-2-incidents))
    (is (= (get-uri-from-test-db "/api" "count=2&min=0")
           first-2-incidents))
    (is (= (get-uri-from-test-db "/api" "count=2&max=0")
           first-2-incidents))
    (testing "ignore broken lat/lng strings")
    (is (= (get-uri-from-test-db "/api" "count=2&lat=0&lng=0")
           first-2-incidents))
    (testing "ignore broken lat/lng strings")
    (is (= (get-uri-from-test-db "/api" "count=2&lat=")
           first-2-incidents))
    (is (= (get-uri-from-test-db "/api" "count=2&lng=0")
           first-2-incidents))))



(deftest count-geos
  (testing "first two geos")
  (is (= (get-uri-from-test-db "/api/geos" "count=2")
         '({:geo nil,
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
            "Occurred at Ocean Shore Elementary School on Oceana Bl. , Pacifica. 911 HANGUP"}))))




(deftest all-geos
  (testing "all the unfiltered geos. should show only UNIQUE geos, with most recent incident for each")
  (is (= (get-uri-from-test-db "/api/geos" "")
         (-> "resources/testdata/geos-api-all.edn"
             slurp
             clojure.edn/read-string))))



(deftest incidents-by-date
  (testing "all the incidents within a date range")
  (is (= (get-uri-from-test-db "/api" "count=5&min=1338366000000&max=1392013560000")
         '({:geo {:lat 37.6511954, :lng -122.4841703},
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
            "Occurred on Arguello Bl, Pacifica. Ticket sign off /rp not able to leave his home/"}))))


(deftest by-geos
  (testing "search by lat/lng")
  (is (= (get-uri-from-test-db "/api" "lat=37.6408391&lng=-122.4903562")
         '({:geo {:lat 37.6408391, :lng -122.4903562},
            :address
            "Ocean Shore Elementary School on Oceana Bl. , Pacifica, CA",
            :time 1367445000000,
            :type "Susp Circ 911",
            :id 130501187,
            :disposition "Checks Ok.",
            :description
            "Occurred at Ocean Shore Elementary School on Oceana Bl. , Pacifica. 911 HANGUP"}))))



#_(deftest by-geo-and-date
    (testing "by geo AND date")
    ;; TODO: find a better lat/lng and date combination in the subset db, that has > 1 record
    (get-uri-from-test-db "/api" "lat=37.6408391&lng=-122.4903562&min=1338366000000&max=1392013560000")
    )


(deftest min-max-dates
  (testing "min and max dates")
  (is (= (get-uri-from-test-db "/api/dates" "")
         {:max 1392406620000, :min 1338760800000})))


(deftest text-search
  (testing "text search")
  (is (= (get-uri-from-test-db "/api" "search=vehicle")
         '({:geo {:lat 37.6509378, :lng -122.4772329},
            :address "Inverness Dr, Pacifica, CA",
            :time 1389787500000,
            :type "Police Mutual Aid",
            :id 140115018,
            :disposition "Report Taken.",
            :description
            "Officer initiated activity at Inverness Dr, Pacifica.1030 veh unoccupied  -- vehicle facing southbound. . "}))))


(deftest type-search
  (testing "type search")
  (is (= (get-uri-from-test-db "/api" "type=Warrant")
         '({:geo {:lat 37.58244, :lng -122.48358},
            :address "Linda Mar Bl/Capistrano Dr, Pacifica, CA",
            :time 1390684740000,
            :type "Warrant Arrest",
            :id 140125130,
            :disposition "Arrest Made.",
            :description
            "Officer initiated activity at Linda Mar Bl/Capistrano Dr, Pacifica."}
           {:geo {:lat 37.6138253, :lng -122.4869194},
            :address "Calera Creek, Cabrillo Hwy, Pacifica, CA",
            :time 1384812420000,
            :type "Warrant Arrest",
            :id 131118154,
            :disposition "Report Taken.",
            :description
            "Officer initiated activity at Calera Creek, Cabrillo Hwy, Pacifica. ."}
           {:geo {:lat 37.63012, :lng -122.48911},
            :address "Oceana Bl/Clarendon Rd, Pacifica, CA",
            :time 1372145460000,
            :type "Warrant Arrest",
            :id 130624261,
            :disposition "Cancelled.",
            :description
            "Officer initiated activity at Oceana Bl/Clarendon Rd, Pacifica. ."}))))


(deftest proper-json
  (testing "for proper json headers")
  (let [correct-content-type "application/json;charset=UTF-8"]
    ;; TODO: unboilerplate this crap
    (is (=  correct-content-type
            (-> (srv/app {:uri "/api/geos"
                          :db (test-db)
                          :request-method :get})
                :headers
                (get "Content-Type"))))
    (is (= correct-content-type
           (-> (srv/app {:uri "/api/dates"
                         :db (test-db)
                         :request-method :get})
               :headers
               (get "Content-Type"))))
    (is (= correct-content-type
           (-> (srv/app {:uri "/api"
                         :db (test-db)
                         :request-method :get})
               :headers
               (get "Content-Type"))))))


(comment

  [1338366000000 1392018300000]


  
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

  ;; TODO: make THESE below into unit tests too. Why not? Tedium, it is the way.
  
  (->> (srv/app {:uri "/api/keys/type"
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode)

  (->> (srv/app {:uri "/api/stats/type"
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode)
  
  (->> (srv/app {:uri "/api/keys/disposition"
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode)


  (->> (srv/app {:uri "/api/stats/disposition"
                 :db (test-db)
                 :request-method :get})
       :body
       keyed-decode)






  
  )

(comment
  ;; live
  (->> (client/get "http://pacifica-incidents.herokuapp.com/api/status" {:as :json})
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
  ;; suck everything down from live.
  ;; um, don't run this on the live site, it'd be pointless.
  (db/recover-from-backup  "http://pacifica-incidents.herokuapp.com/api")


  ;; suck everything down from backup
  (db/recover-from-backup  "/tmp/backupdata.json")
  
            

  )


(comment

  (run-tests)

  )