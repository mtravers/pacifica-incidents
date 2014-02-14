(ns incidents.parse
  (:require [instaparse.core :as ip]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [utilza.repl :as urepl]))



(defn page-delim-hack
  [s]
  (s/replace s #"\nPage.*?\n\d+/\d+/\d+\n" ""))


;; TODO: time, which will need to be added to date.
(defn transforms
  [t]
  (ip/transform
   {:id (comp clojure.edn/read-string str)}
        t))


(comment

  ;; this tests the results and dumps it to output.edn as a hack
  ;; to pretty-print it because otherwise it's an unreadable mess
  ;; set up a buffer with /tmp/output.edn as an auto-revert-mode
  

  (->> (ip/parse
        (ip/parser (slurp "resources/ppd.bnf"))
        (->  "resources/testdata/well-formed.txt"
             slurp
             page-delim-hack))
       ;;transforms
       (urepl/massive-spew "/tmp/output.edn"))

  (->> (ip/parse
        (ip/parser (slurp "resources/ppd.bnf"))
        (->  "resources/testdata/well-formed.txt"
             slurp
             page-delim-hack)        
        :unhide :all) ;; for debuggging!
       ;; transforms
       (urepl/massive-spew "/tmp/output.edn"))

  



  
  (ip/parse
   (ip/parser (slurp "resources/ppd.bnf"))
   (slurp "resources/testdata/poorly-formed.txt"))
  

  
  
  )