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


  (->> (srv/app {:uri "/api/status"
                 :request-method :get})
       :body
       json/decode)


  (->> (srv/app {:uri "/api"
                 :request-method :get
                 :query-string "count=2"})
       :body
       json/decode)
  

  (->> (srv/app {:uri "/api"
                 :request-method :get
                 :query-string "count=5&min=1338366000000&max=1392013560000"})
       :body
       json/decode)
  

  (->> (srv/app {:uri "/api/dates"
                 :request-method :get})
       :body
       json/decode)
  

  
  )

(comment
  ;; live
  (->> (client/get "http://incidents.bamfic.com/api/status" {:as :json})
       :body)


  )
