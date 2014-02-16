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
    (when-not (= status 200)
      (throw (Exception. (str "Status " status))))
    (when (:error_message body)
      (throw (Exception. (:error_message body))))
    (Thread/sleep 100)                  ;rate limit
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
    (println match)
    (str match " Pacifica, CA")))

;; TODO:  handle exceptional case of no valid address found in text.
(defn add-geo
  "Add geo to item map, if there is a valid geo response from server.
  Otherwise returns item map unchanged."
  [{:keys [description] :as item}]
  (or (some->> description
               find-address
               geocode-address
               (assoc item :geo))
      item))

(defn update-geos!
  "Geocode everything in the db
  that doesn't already have a geo"
  []
  (doseq [item @db/db]
    (when (-> item :geo empty?)
      (add-geo item)))
  (db/save-data!))



(comment
  (db/read-data!)
  ;; do it in a separate thread, which is killable.
  (future (update-geos!))
  (future-cancel *1)
  
  )





