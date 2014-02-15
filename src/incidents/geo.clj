(ns incidents.geo
  (:require [clj-http.client :as client]))

;; TODO: move to env
(def *geocoding-url* "http://maps.googleapis.com/maps/api/geocode/json")

;; TODO: try/catch transient http errors
(defn geocode-address
  [addr]
  ;; assuming status is needed here for something.
  ;; otherwise, could ditch the let and just (-> (client/get ...) :body) instead
  (let [{:keys [status body]} (client/get *geocoding-url*
                                          {:query-params {:address addr, :sensor false}
                                           :as :json})]
    body))

(defn find-address
  [s]
  (when-let [match (-> #".*?(at|on) (.*)Pacifica.*"
                       re-matches
                       (nth 2))]
    (str match " CA")))



;; TODO: the format of the returned structure from parse, will be a proper keyword map,
;; not the weird nested vectors it is now. At that time, the second and filter won't be needed.
(defn add-geo
  [item]
  (map #(-> %
            second
            find-address
            geocode-address)
       (filter #(= :stuff (first %)) (rest item)))







  )

(comment
  

  )





