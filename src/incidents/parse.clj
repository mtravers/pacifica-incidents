(ns incidents.parse
  (:require [instaparse.core :as ip]))






(comment


  (ip/parse
   (ip/parser (slurp "resources/ppd.bnf"))
   (slurp "resources/testdata/PPDdailymediabulletin2014-02-09.txt"))
              

  
  )