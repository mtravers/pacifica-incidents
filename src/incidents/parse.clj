(ns incidents.parse
  (:require [instaparse.core :as ip]))






(comment

  (ip/parse
   (ip/parser (slurp "resources/ppd.bnf"))
   (slurp "resources/testdata/well-formed.txt"))
  
  (ip/parse
   (ip/parser (slurp "resources/ppd.bnf"))
   (slurp "resources/testdata/poorly-formed.txt"))
              

  
  )