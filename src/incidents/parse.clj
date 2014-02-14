(ns incidents.parse
  (:require [instaparse.core :as ip]
            [utilza.repl :as urepl]))






(comment

  ;; this tests the results and dumps it to output.edn as a hack
  ;; to pretty-print it because otherwise it's an unreadable mess
  ;; set up a buffer with /tmp/output.edn as an auto-revert-mode
  
  (->> (ip/parse
        (ip/parser (slurp "resources/ppd.bnf"))
        (slurp "resources/testdata/well-formed.txt"))
       (urepl/massive-spew "/tmp/output.edn"))

  (->> (ip/parse
        (ip/parser (slurp "resources/ppd.bnf"))
        (slurp "resources/testdata/well-formed.txt")
        :unhide :all) ;; for debuggging!
       (urepl/massive-spew "/tmp/output.edn"))


  
  (ip/parse
   (ip/parser (slurp "resources/ppd.bnf"))
   (slurp "resources/testdata/poorly-formed.txt"))
  

  
  
  )