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


(defn yank-disposition
  "The disposition is un-possible for me to pull out using instaparse
   without either crashing the parser or causing an endless loop requiring killign the JVM.
   So, just do it via brute force and regexps after parsing is done"
  [m]
  (let [{:keys [description]} m
        [_ new-description disposition] (re-matches #"(.*?) Disposition: (.*)" description)]
    (merge m {:description new-description
              :disposition disposition})))


(defn munge-rec
  "Takes a rec which is [[k v]] pairs with the :rec stripped off.
   Merges it into a map with the :description concatenated."
  [rec]
  (reduce (fn [acc [k v]]
            (if (= k :description)
              (merge-with #(apply str (interpose " "  %&)) acc {:description v})
              (assoc acc k v)))
          {}
          rec))






;; TODO: next, the timezones *sigh*
(def transforms {:id  (comp clojure.edn/read-string str)
                 :hdate #(tfmt/parse
                          (tfmt/formatter "MMMM d, yyyy") %)
                 :time #(tfmt/parse
                         (tfmt/formatters :hour-minute) %)})


(defn parse-tree
  [t])


(comment

  ;; this tests the results and dumps it to output.edn as a hack
  ;; to pretty-print it because otherwise it's an unreadable mess
  ;; set up a buffer with /tmp/output.edn as an auto-revert-mode
  

  (->> (ip/parse
        (ip/parser (slurp "resources/ppd.bnf"))
        (->  "resources/testdata/well-formed.txt"
             slurp
             page-delim-hack))
       rest
       (reduce (fn [acc [k & vs]]
                 (if (= :rec k)
                   (update-in acc [:recs] conj (-> vs munge-rec yank-disposition))
                   (assoc acc k vs)))
                   {:recs []})
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


  )