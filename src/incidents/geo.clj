(ns incidents.geo
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as client])
  )

(def *geocoding-url* "http://maps.googleapis.com/maps/api/geocode/json")

(defn geocode-address [addr]
  (let [resp (client/get *geocoding-url* {:query-params {"address" addr, "sensor" "false"}})
        status (:status resp)
        results (json/read-str (:body resp))]
    results))
   
(defn find-address [s]
  (let [match (nth (re-matches #".*?(at|on) (.*)Pacifica.*" s) 2)]
    (if match (str match " CA"))))

(defn add-geo [item]
  (map #(-> %
            second
            find-address
            geocode-address)
       (filter #(= :stuff (first %)) (rest item)))







  )

(comment
  

  )




  
