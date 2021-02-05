(ns incidents.ocr
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [me.raynes.fs :as fs]
            [org.parkerici.multitool.core :as u]
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
  "Turns a set of json blocks into a set of trees, with PAGEs as top elements"
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
        {:pages
         (filter (fn [b] (not (some (fn [ob] (some (fn [child] (= child b)) (:children ob))) direct)))
                 direct)
         :index block-index}))))

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
  [block-index table]
  (prn :parse-table (count block-index) (count (:children table)))
  (->> table
       :children
       (group-by :RowIndex)
       (into (sorted-map))
       vals
       (map parse-row)
       (u/partition-if #(let [id (get % 2)]
                          (and (not (empty? id))
                               (u/coerce-numeric-hard id))))))
 
;;; → multitool
(defn partition-diff
  "Partition coll by calling f on adjacent pairs of elements, if true elts are in same partition"
  [f coll]
  (loop [partitions []
         current [(first coll)]
         tail (rest coll)]
    (cond (empty? tail) (conj partitions current)
          (f (last current) (first tail))
          (recur partitions
                 (conj current (first tail))
                 (rest tail))
          true
          (recur (conj partitions current)
                 [(first tail)]
                 (rest tail)))))

(defn parse-nontable
  [block-index page-blocks]
  (prn :parse-nontable (count block-index) (count (:children page-blocks)))
  (let [block-top #(get-in % [:Geometry :BoundingBox :Top])
        lines (partition-diff (fn [a b] (< (- (block-top b) (block-top a)) 0.01))
                              (sort-by block-top (:children page-blocks)))
        items (u/partition-if (fn [line] (u/coerce-numeric-hard (:Text (first line)))))
        ]
    items))

(defn parse-page
  [block-index page]
  (if-let [table (u/walk-find #(= (:BlockType %) "TABLE") page)]
    (parse-table block-index table)
    (parse-nontable block-index page)))

(defn entries
  [{:keys [pages index]}]
  (mapcat (partial parse-page index) pages))

(defn parse-entry
  [[line1 line2]]
  (let [[_ time type] (re-matches #"(\d+:\d+)\s+(.*?)" (second line1))
        [_ location disposition] (re-matches #"(?:.*) (?:on|at) (.*) Disposition: (.*)" (second line2))]
    {:id (nth line1 2)
     :time time
     :type type
     :location location
     :disposition disposition
     }))

(comment

(->> ["data/parsed/01-23-2021.json" "data/parsed/01-23-2021a.json"]
     (map read-file)
     (mapcat :Blocks)
     direct
     entries)

     rest
     (map parse-entry))


;;; TODO also should maybe get the date.
;
(doseq [f (fs/list-dir "data/parsed")]
  (prn f)
  (->> f
       read-file
       parse-textract))
)

(map read-file )
  
