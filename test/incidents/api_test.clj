(ns incidents.api-test
  (:import org.joda.time.DateTime)
  (:require [clojure.test :refer :all]
            [clj-http.client :as client]
            [incidents.server :as srv]
            [utilza.repl :as urepl])
  (:use incidents.api))


(comment

  [1338366000000 1392018300000]

  
  (get-all {:count "1"
            :min "1338366000000"
            :max "1392013560000"})


  (get-all {:count "1"})


  #_(srv/app {:uri "/api/status"
              :request-method :get})

  
  #_(srv/app {:uri "/api"
              :request-method :get
              :query-params {:count 1}})
  
  )
