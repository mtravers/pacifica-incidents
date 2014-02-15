(ns incidents.geo
  (:require [clj-http.client :as client]
            [incidents.db :as db]
            [environ.core :as env]))



;; TODO: try/catch transient http errors
(defn geocode-address
  [addr]
  ;; assuming status is needed here for something.
  ;; otherwise, could ditch the let and just (-> (client/get ...) :body) instead
  (let [{:keys [status body]} (client/get (:geocoding-url env/env)
                                          {:query-params {:address addr, :sensor false}
                                           :as :json})]
    ;; Most likely only really want the first result anyway.
    (-> body
        :results
        first)))

(defn find-address
  [s]
  (when-let [match (-> #".*?(at|on) (.*)Pacifica.*"
                       (re-matches s)
                       (nth 2))]
    ;; the Pacifica needs to be there so that it doesn't pull
    ;; up Monterey road in Monterey, for example.
    (str match " Pacifica, CA")))



;; TODO:  handle exceptional case of no valid address found in text.
(defn add-geo
  [{:keys [description] :as item}]
  (assoc item :geo (some-> description
                           find-address
                           geocode-address)))

(defn update-geos
  "Geocode everything in the db
  that doesn't already have a geo"
	[]
  (doseq [item @db/db]
    (when (-> item :geo empty?)
      (swap! db/db add-geo)))
	(db/save-data))

  



(comment


  )





