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
    (-> body
        :results
        first)))

(defn find-address
  [s]
  (when-let [match (-> #".*?(at|on) (.*)Pacifica.*"
                       (re-matches s)
                       (nth 2))]
    (str match " Pacifica, CA")))



;; TODO:  handle exceptional case of no valid address found in text.
(defn add-geo
  [{:keys [description] :as item}]
  (assoc item :geo (-> description
                       find-address
                       geocode-address)))

(defn add-geos
  [items]
  (map add-geo items))



(comment


  )





