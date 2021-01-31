(ns incidents.ocr
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as s]))

;;; Parsing AWS Textract output into usable form

(defn read-file [f]
  (with-open [s (io/reader f)]
    (json/read s :key-fn keyword)))

(defn index-by 
  "Return a map of the elements of coll indexed by (f elt). Similar to group-by, but overwrites elts with same index rather than producing vectors "
  [f coll]  
  (zipmap (map f coll) coll))

(defn table [js]
  (->> js
       :Blocks
       (filter #(= (:BlockType %) "TABLE"))
       first))

(defn children [block]
  (get-in block [:Relationships 0 :Ids])) ;note: assumes there are no other :Relationships, which seems true

(defn parse-table
  [js]
  (let [blocks (index-by :Id (:Blocks js))
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

(defn rows
  [p]
  (->> p
       :children
       (group-by :RowIndex)
       vals
       (map parse-row)))

#_ (-> "test/resources/textract-output.js"
       read-file
       parse
       rows
       )


