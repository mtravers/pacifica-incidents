(ns incidents.ocr
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as s]
            [org.parkerici.multitool.core :as u]
            ))

;;; Parsing AWS Textract output into usable form
;;; TODO! only gets first page

;;; TODO → utils
(defn partition-if
  [f coll]
  (lazy-seq
   (when-let [s (seq coll)]
     (let [fst (first s)
           fv (f fst)
           run (cons fst (take-while #(not (f %)) (next s)))]
       (cons run (partition-if f (lazy-seq (drop (count run) s))))))))

(defn read-file [f]
  (with-open [s (io/reader f)]
    (json/read s :key-fn keyword)))

(defn table [js]
  (->> js
       :Blocks
       (filter #(= (:BlockType %) "TABLE"))
       first))

(defn children [block]
  (get-in block [:Relationships 0 :Ids])) ;note: assumes there are no other :Relationships, which seems true

(defn parse-table
  [js]
  (let [blocks (u/index-by :Id (:Blocks js))
        table (table js)]
    (letfn [(direct-children [b]
              (-> b
                  (assoc 
                     :children
                     (map (fn [child-id]
                            (-> child-id
                                 blocks
                                 direct-children
                                 ))
                          (children b)))
                  (select-keys [:children :Text :ColumnSpan :ColumnIndex :RowSpan :RowIndex :Id :BlockType])))]
      (direct-children table))))
            
    
(defn cell-contents
  [cell]
  (s/join \space (map :Text (:children cell))))

(defn parse-row
  [row]
  (->> row
       (sort-by :ColumnIndex)
       (map cell-contents)))

(defn entries
  [p]
  (->> p
       :children
       (group-by :RowIndex)
       (into (sorted-map))
       vals
       (map parse-row)
       (partition-if #(let [id (nth % 2)]
                        (and (not (empty? id))
                             (u/coerce-numeric-hard id))))))


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


#_
(->> "resources/testdata/textract-output.js"
     read-file
     parse-table
     entries
     rest
     (map parse-entry))


;;; TODO also should maybe get the date.
;
