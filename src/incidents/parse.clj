(ns incidents.parse
  (:require [instaparse.core :as ip]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clojure.walk :as walk]
            [utilza.misc :as umisc]
            [utilza.repl :as urepl]))



;; XXX this wouldn't be necessary if there were a way
;; to multi-pass through the parser: once to get the page data out
;; of the records, and then to parse the records.
;; I know how to do that with regexps, but not within EBNF.
(defn page-delim-hack
  "XXX this is horrible."
  [s]
  (s/replace s #"\nPage.*?\n\d+/\d+/\d+\n" ""))


;; TODO:  the dates/times are going to be interesting. clj-time fo sho.
(def transforms {:id  (comp clojure.edn/read-string str)})

(defn transform-all
  [t]
  (walk/postwalk  (fn [d]
                    (if  (vector? d)
                      (let [[k v] d]
                        (if (k transforms)
                          [k ((k transforms) v)]
                          d))
                      d))
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
       transform-all
       (urepl/massive-spew "/tmp/output.edn"))


  
  (->> (ip/parse
        (ip/parser (slurp "resources/ppd.bnf"))
        (->  "resources/testdata/well-formed.txt"
             slurp
             page-delim-hack)        
        :unhide :all) ;; for debuggging!
       transform-all
       (urepl/massive-spew "/tmp/output.edn"))

  



  
  (ip/parse
   (ip/parser (slurp "resources/ppd.bnf"))
   (slurp "resources/testdata/poorly-formed.txt"))
  

  
  
  )