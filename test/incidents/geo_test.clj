(ns incidents.geo-test
  (:import org.joda.time.DateTime)
  (:require [clojure.test :refer :all]
            [utilza.repl :as urepl]
            [incidents.geo :refer :all]))


(comment

    (->> "resources/testdata/broken-geo.edn"
       slurp
       clojure.edn/read-string
       :geo
       :geometry
       :location)



    
    )