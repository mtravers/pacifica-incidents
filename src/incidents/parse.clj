(ns incidents.parse
  (:import org.joda.time.DateTime
           java.util.Date)
  (:require [instaparse.core :as ip]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clj-time.format :as tfmt]
            [clj-time.core :as time]
            [clojure.walk :as walk]
            [utilza.misc :as umisc]
            [utilza.repl :as urepl]))



;; XXX this wouldn't be necessary if there were a way
;; to multi-pass through the parser: once to get the page data out
;; of the records, and then to parse the records.
;; I know how to do that with regexps, but not within EBNF.
(defn- page-delim-hack
  "XXX this is horrible."
  [s]
  (s/replace s #"\nPage.*?\n\d+/\d+/\d+\n" ""))

;; TODO: get rid of this too:
;; "PDF created with pdfFactory trial version www.pdffactory.com \f",


(defn- yank-disposition
  "The disposition is un-possible for me to pull out using instaparse
   without either crashing the parser or causing an endless loop requiring killign the JVM.
   So, just do it via brute force and regexps after parsing is done"
  [m]
  (let [{:keys [description]} m
        [_ new-description disposition] (re-matches #"(.*?) Disposition: (.*)" description)]
    (merge m {:description new-description
              :disposition disposition})))


(defn- munge-rec
  "Takes a rec which is [[k v]] pairs, with duplicate k's for things like :description.
   Merges it into a map with the :description concatenated."
  [rec]
  (reduce (fn [acc [k v]]
            (if (= k :description)
              (merge-with #(apply str (interpose " "  %&)) acc {:description v})
              (assoc acc k v)))
          {}
          rec))


(defn- fix-time
  "Takes a DateTime of the date, and a DateTime of the time
    and sums them. Date coerces to UTC but the test suite shows
    they are correct."
  [d t]
  (->> [d t]
       (map #(.getMillis %))
       (apply +)
       Date.))

(defn- fix-times
  [data]
  (let [{:keys [date recs]} data]
    (map (fn [m] (update-in m  [:time] #(fix-time date %))) recs)))


(def transforms {:id  (comp clojure.edn/read-string str)
                 :time #(tfmt/parse
                         (tfmt/with-zone
                           (tfmt/formatters :hour-minute)
                           (time/time-zone-for-id "America/Los_Angeles"))
                         %)})


(defn fix-ids
  "Takes a list of [k v] tuples, filters the id's only, formats them as nums,
   and returns them as a list"
  [id-tuples]
  (for [[k v] id-tuples
        :when (= :id k)]
    (-> v
        str
        clojure.edn/read-string)))


(defn zip-ids-recs
  "Associate id's with their records. Leaves tree structure intact for date fix later."
  [{:keys [ids recs] :as t}]
  (-> t
      (dissoc :ids)
      (assoc  :recs (map #(assoc %1 :id  %2) recs ids))))

(defn- parse-tree
  "Takes a tree in the shape [:recs [k v] [k v]] and returns a map of {:date xxx :recs [m1 m2 m3...]}
   with all the recs maps formatted properly."
  [[_ & recs]]
  (reduce (fn [acc [k & vs]]
            (case k
              :rec (update-in acc [:recs] conj (->> vs
                                                    munge-rec
                                                    yank-disposition
                                                    (umisc/munge-columns transforms)))
              :hdate (assoc acc :date (tfmt/parse
                                       (tfmt/formatter "MMMM d, yyyy") (first vs)))
              :pagedelim (update-in acc [:ids] concat (fix-ids vs))
              :else (assoc acc k vs))) ;; really shouldn't happen. throw an error instead?
          {:recs []}
          recs))



(defn parse-pdf-text
  "Takes a string of the PDF, and parses it out. Returns a tree with parsed data."
  [s]
  (->> s
       page-delim-hack
       (ip/parse
        (ip/parser (slurp "resources/ppd.bnf")))
       parse-tree
       fix-times))



(comment

  ;; this tests the results and dumps it to output.edn as a hack
  ;; to pretty-print it because otherwise it's an unreadable mess
  ;; set up a buffer with /tmp/output.edn as an auto-revert-mode,
  ;; then eval the below form(s) to do the parsing.
  

  (->>  "resources/testdata/well-formed.txt"
        slurp
        parse-pdf-text
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
       (urepl/massive-spew "/tmp/output.edn"))

  
  

  (->> (ip/parse
        (ip/parser (slurp "resources/ppd-bad.bnf"))
        (->  "resources/testdata/poorly-formed.txt"
             slurp)
        ;; for debuggging!
        :total true
        :unhide :all) 
       (urepl/massive-spew "/tmp/output.edn"))
  


  
  (->> (ip/parse
        (ip/parser (slurp "resources/ppd-bad.bnf"))
        (->  "resources/testdata/poorly-formed.txt"
             slurp))
       parse-tree
       zip-ids-recs
       fix-times
       (urepl/massive-spew "/tmp/output.edn"))
  
  
  )


(comment


  )