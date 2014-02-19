(ns incidents.geo-test
  (:import java.util.Date
           org.joda.time.DateTime)
  (:require [clojure.test :refer :all]
            [utilza.repl :as urepl]
            [incidents.db :as db]
            [clojure.tools.trace :as trace]
            [incidents.geo :refer :all]))


(comment

  (->> "resources/testdata/broken-geo.edn"
       slurp
       clojure.edn/read-string
       :geo
       :geometry
       :location)


  (->> @db/db
       vals
       (remove :geo)
       rand-nth
       :address
       geocode-address)
  


  (->> @db/db
       vals
       (remove :geo)
       rand-nth
       :address
       find-existing-geo)

  (->> @db/db
       vals
       (remove :geo)
       rand-nth
       add-geo-and-address)

  ;; awesomely useful for debugging.
  (trace/trace-vars copy-or-fetch-geo)
  (trace/untrace-vars copy-or-fetch-geo)


  ;; all the records with valid addresses but no geos
  ;; these are SUPPOSED to be caught by add-geo-and-address and fetched from google!
  (->> @db/db
       vals
       (filter :address)
       (remove (comp find-existing-geo :address))
       (urepl/massive-spew "/tmp/output.edn"))

  ;; this one does not? why not?
  (def no-geo-at-all {:address "Barton Pl. , Pacifica, CA",
                      :id "120613195",
                      :disposition "Log Note Only.",
                      :description
                      "Occurred on Barton Pl. , Pacifica. Service Class: VOIP-911 hang up, on call back, X answered advises that her husband was having a pain in his side but she disconnected bc he doesn't want medics. .",
                      :type "Susp Circ 911",
                      :time (DateTime. "2012-06-14T01:36:00.000-00:00")})

  ;; this should hit the network if there's nothing in the db yet.
  (->> no-geo-at-all
       add-geo-and-address)



  (->> (for [id (-> @db/db keys shuffle)
             :when (and (some-> id nil? not) ;; there's one bad id in there
                        (-> (get @db/db id nil)
                            nil?
                            not))]
         id)
       count)

  ;; this is good
  (some->> no-geo-at-all
           :geo
           nil?) 


  (let [{:keys [id]} no-geo-at-all]
    (-> (get @db/db id nil)
        nil?
        not))
  
  ;; how many so far
  (->> @db/db
       vals
       (filter :geo)
       count)

  ;; example one
  (->> 130927231
       (get @db/db))

  
  )