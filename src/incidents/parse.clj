(ns incidents.parse
  (:import org.joda.time.DateTime
           java.util.Date
           java.net.URL
           org.apache.pdfbox.pdmodel.PDDocument
           org.apache.pdfbox.util.PDFTextStripper)
  (:require [instaparse.core :as ip]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clj-http.client :as client]
            [taoensso.timbre :as log]
            [clojure.stacktrace :as st]
            [clj-time.format :as tfmt]
            [clj-time.core :as time]
            [clojure.walk :as walk]
            [utilza.misc :as umisc]
            [utilza.repl :as urepl]))


(defn pdf-to-text
  "Takes a string with an url. Turns that pdf into sweet, sweet text"
  [url]
  (with-open [d (-> url java.net.URL. PDDocument/load)]
    (-> (doto (PDFTextStripper. "UTF-8")
          (.setForceParsing true)
          (.setSortByPosition false)
          (.setShouldSeparateByBeads true))
        (.getText  d))))


;; XXX this wouldn't be necessary if there were a way
;; to multi-pass through the parser: once to get the page data out
;; of the records, and then to parse the records.
;; I know how to do that with regexps, but not within EBNF.
(defn- page-delim-hack
  "XXX this is horrible."
  [s]
  (s/replace s #"\nPage.*?\n\d+/\d+/\d+\n" ""))


(defn- no-f-hack
  [s]
  (s/replace s "\f" "\n"))

(defn- brutal-page-delim-hack
  "XXX this is horrible."
  [s]
  (-> s
      (s/replace  #"\nPage.*?\n" "\n")
      (s/replace  #"\n\d+/\d+/\d+\n" "\n")
      (s/replace #"\nPDF created with pdfFactory.*\n" "\n")))

(defn fix-stupid-pdf
  [s]
  (some-> s 
          (clojure.string/split  #"PDF created with pdfFactory.*")
          first))


(defn- yank-disposition
  "The disposition is un-possible for me to pull out using instaparse
   without either crashing the parser or causing an endless loop requiring killign the JVM.
   So, just do it via brute force and regexps after parsing is done"
  [m]
  (let [{:keys [description]} m
        [_ new-description disposition] (re-matches #"(.*?) Disposition: (.*)" description)]
    (merge m {:description new-description
              :disposition (fix-stupid-pdf disposition)})))



(defn- parse-topline
  [s]
  {:post [(->> % vals (every? (comp not nil?)))]}
  (->> s
       (re-matches #"(\d+:\d+)\s+(.*?)\s+(\d+)\n")
       rest
       (zipmap [:time :type :id])))


(defn- yank-topline
  "instaparse is puking blood when faced with this topline.
   Just go around it with regexps, at least those work"
  [m]
  (let [{:keys [topline]} m]
    (if topline
      (-> m
          (merge  (parse-topline topline))
          (dissoc :topline))
      m)))


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

(defn parse-time
  [t]
  (tfmt/parse
   (tfmt/with-zone
     (tfmt/formatters :hour-minute)
     (time/time-zone-for-id "America/Los_Angeles"))
   t))

(defn- fix-times
  "Takes a structure with a key :date with the date,
   and :recs with a seq of all the incidents for that date.
   Assigns the :time key in the recs to be the correct datetime with teh date AND time combined,
   and returns the incidents with those assoced in."
  [{:keys [date recs]}]
  (map (fn [m]
         (-> m
             (update-in  [:time] #(fix-time date %))
             (dissoc :hdate)))
       recs))


(def transforms {:id  (comp clojure.edn/read-string str) ;; TODO: parseLong?
                 :time parse-time})



(defn- parse-tree
  "Takes a tree in the shape [:recs [k v] [k v]] and returns a map of {:date xxx :recs [m1 m2 m3...]}
   with all the recs maps formatted properly."
  [[_ & recs]]
  (reduce (fn [acc [k & vs]]
            (case k
              :rec (update-in acc [:recs] conj (->> vs
                                                    munge-rec
                                                    yank-disposition
                                                    yank-topline
                                                    (umisc/munge-columns transforms)))
              :hdate (assoc acc :date (tfmt/parse
                                       (tfmt/formatter "MMMM d, yyyy") (first vs)))
              :else (assoc acc k vs))) ;; really shouldn't happen. throw an error instead?
          {:recs []}
          recs))




(defn parse-with-failure-log
  [parser-file s]
  (let [p (-> parser-file
              ip/parser
              (ip/parse s))]
    (if (ip/failure? p)
      (let [estr  (str parser-file  (-> p ip/get-failure pr-str) s)]
        ;;(log/debug estr)
        (throw (Exception. estr)))
      p)))



(defn parse-pdfbox-text
  [s]
  (->> s
       brutal-page-delim-hack
       no-f-hack
       (parse-with-failure-log "resources/pdfbox.bnf")
       parse-tree
       fix-times))



(defn parse-pdf-text
  "Takes a string of a text-extracted PDF, and parses it out. Returns a tree with parsed data."
  [s]
  (try
    (parse-pdfbox-text s)
    (catch Exception e
      (log/error e))))










