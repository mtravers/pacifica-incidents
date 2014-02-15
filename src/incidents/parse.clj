(ns incidents.parse
  (:import org.joda.time.DateTime)
  (:require [instaparse.core :as ip]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clj-time.format :as tfmt]
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


;; TODO: next, the timezones *sigh*
(def transforms {:id  (comp clojure.edn/read-string str)
                 :hdate #(tfmt/parse
                          (tfmt/formatter "MMMM d, yyyy") %)
                 :time #(tfmt/parse
                         (tfmt/formatters :hour-minute) %)})

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



(defn munge-rec
  "Takes a rec which is [[k v]] pairs with the :rec stripped off.
   Merges it into a map with the :stuff concatenated."
  [rec]
    (reduce (fn [acc [k v]]
              (if (= k :stuff)
                (merge-with str acc {:stuff v})
                (assoc acc k v)))
            {}
            rec))

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


    ;; debug version
    (->> (ip/parse
          (ip/parser (slurp "resources/ppd.bnf"))
          (->  "resources/testdata/well-formed.txt"
               slurp
               page-delim-hack)
          ;; for debuggging!
          :total true
          :unhide :all) 
         ;;transform-all ;; don't do the transforms becasue the errors choke it.
         (urepl/massive-spew "/tmp/output.edn"))

    
    

    ;; fail-o-rama
    (ip/parse
     (ip/parser (slurp "resources/ppd.bnf"))
     (slurp "resources/testdata/poorly-formed.txt"))
    

    
    
    )


  (comment
    (tfmt/show-formatters)


    ;; sample rec

    
    
    )