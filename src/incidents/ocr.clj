(ns incidents.ocr
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [me.raynes.fs :as fs]
            [org.parkerici.multitool.core :as u]
            [taoensso.timbre :as log]
            ))

;;; Parsing AWS Textract output into usable form
;;; TODO! only gets first page

(defn read-file [f]
  (with-open [s (io/reader f)]
    (json/read s :key-fn keyword)))

;;; Low level parse: turns block list into nested structure with only the fields we care about

(defn table [js]
  (->> js
       :Blocks
       (filter #(= (:BlockType %) "TABLE"))
       first))

(defn children [block]
  (get-in block [:Relationships 0 :Ids])) ;note: assumes there are no other :Relationships, which seems true

(defn direct
  "Turns a set of json blocks into a set of trees, with PAGEs as top elements and sub-blocks on the :children attribute"
  [blocks]
  (let [block-index (u/index-by :Id blocks)]
    (letfn [(direct-children [b]
              (-> b
                  (assoc 
                   :children
                   (map (fn [child-id]
                          (-> child-id
                              block-index
                              (or (prn :not-found child-id)) ;Should't happen if all parts have been downloaded
                              direct-children
                              ))
                        (children b)))
                  (select-keys [:children :Text :ColumnSpan :ColumnIndex :RowSpan :RowIndex :Id :BlockType :Geometry])))]
      (let [direct (map direct-children blocks)]
        ;; Would be easier to just selct for PAGE, but oh well
        (filter (fn [b] (not (some (fn [ob] (some (fn [child] (= child b)) (:children ob))) direct)))
                 direct)))))

;;; High level parse – turns low-level parse output into blotter entries

(defn cell-contents
  [cell]
  (s/join \space (map :Text (:children cell))))

(defn parse-row
  [row]
  (->> row
       (sort-by :ColumnIndex)
       (map cell-contents)))

(defn parse-table
  [table]
  (->> table
       :children
       (group-by :RowIndex)
       (into (sorted-map))
       vals
       (map parse-row)
       ;; Split on time, it's reliably detected while the id on the right is not
       (u/partition-if #(re-matches #"^\d\d:\d\d.*" (second %)))))
 
(defn parse-nontable
  [page-blocks]
  (let [block-top #(get-in % [:Geometry :BoundingBox :Top])
        lines (u/partition-diff (fn [a b] (< (- (block-top b) (block-top a)) 0.01))
                              (sort-by block-top (:children page-blocks)))
        items (u/partition-if (fn [line] (u/coerce-numeric-hard (:Text (first line)))))
        ]
    items))

(defn parse-page
  [page]
  (log/info "Parsing page" (:Id page))
  (if-let [table (u/walk-find #(= (:BlockType %) "TABLE") page)]
    (parse-table table)
    (parse-nontable page)))

(defn entries
  [pages]
  (mapcat parse-page pages))

(defn parse-entry
  [[line1 line2]]
  (let [[_ time type] (and (second line2) (re-matches #"(\d+:\d+)\s+(.*?)" (second line1)))
        [_ location disposition] (and (second line2) (re-matches #"(?:.*) (?:on|at) (.*) Disposition: (.*)" (second line2)))]
    (u/clean-map
     {:id (nth line1 2)
      :time time
      :type type
      :location location
      :disposition disposition
      })))

(defn parse-textract
  [tx]
  (->> tx
       direct
       entries
       rest
       (map parse-entry)))



(comment

;;; Now with AWS API we have all in one file and already list-of-blocks edn form

  
)
