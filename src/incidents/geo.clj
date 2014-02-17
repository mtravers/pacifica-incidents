(ns incidents.geo
  (:require [clj-http.client :as client]
            [incidents.db :as db]
            [taoensso.timbre :as log]
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
    (log/debug match)
    (str match " Pacifica, CA")))

;; TODO:  handle exceptional case of no valid address found in text.
(defn get-geo
  [description]
  (some-> description
          find-address
          geocode-address))

(defn update-geo
  "Returns a function to update the db with the geocode for the record
   in the db with id supplied. Suitable for supplying to swap!"
  [id]
  (fn [db]
    (assoc-in db [id :geo] (get-geo (get-in db [id :description])))))

(defn update-geos!
  "Geocode everything in the db
  that doesn't already have a geo"
  []
  (doseq [id (keys @db/db)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :geo empty?))]
    (log/debug "adding geo for " id)
    (swap! db/db  (update-geo id))
    (Thread/sleep 1000))
  (db/save-data!))



(comment
  ;; for debugging
  (log/set-config! [:appenders :spit :enabled?] true)
  (log/set-config! [:shared-appender-config :spit-filename] (:log-filename env/env))
  (log/set-level! :debug)

  
  (db/read-data!)
  ;; do it in a separate thread, which is killable.
  (def running-update (future (update-geos!)))
  (future-cancel running-update)

  ;; how many so far
  (->> @db/db
       vals
       (filter :geo)
       count)

  
  )





