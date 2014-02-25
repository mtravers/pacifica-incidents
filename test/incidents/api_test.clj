(ns incidents.api-test
  (:import org.joda.time.DateTime)
  (:require [clojure.test :refer :all]
            [clj-http.client :as client]
            [incidents.server :as srv]
            [cheshire.core :as json]
            [incidents.reports :as reports]
            [utilza.repl :as urepl])
  (:use incidents.api))


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
                 :request-method :get})
       :body
       json/decode)


  ;; (spit "/tmp/foo.js")
  
  (->> (srv/app {:uri "/api"
                 :request-method :get
                 :query-string "count=2"})
       :body
       json/decode)

  (->> (srv/app {:uri "/api/geos"
                 :request-method :get
                 :query-string "count=2"})
       :body
       json/decode)  



  (->> (srv/app {:uri "/api/geos"
                 :request-method :get})
       :body
       json/decode
       count)
  
  (->> (srv/app {:uri "/api"
                 :request-method :get
                 :query-string "count=5&min=1338366000000&max=1392013560000"})
       :body
       json/decode)

  
  (->> (srv/app {:uri "/api"
                 :request-method :get
                 :query-string "lat=37.6408391&lng=-122.4903562"})
       :body
       json/decode)
  
  (->> (srv/app {:uri "/api"
                 :request-method :get
                 :query-string "lat=37.6408391&lng=-122.4903562&min=1338366000000&max=1392013560000"})
       :body
       json/decode
       count)

  (->> (srv/app {:uri "/api/dates"
                 :request-method :get})
       :body
       json/decode)
  
  (->> (srv/app {:uri "/api/types"
                 :request-method :get})
       :body
       json/decode)

  (->> (srv/app {:uri "/api/types/stats"
                 :request-method :get})
       :body
       json/decode)
  
  (->> (srv/app {:uri "/api/dispositions"
                 :request-method :get})
       :body
       json/decode)


  (->> (srv/app {:uri "/api/dispositions/stats"
                 :request-method :get})
       :body
       json/decode)
  
  )

(comment
  ;; live
  (->> (client/get "http://incidents.bamfic.com/api/status" {:as :json})
       :body)


  )
