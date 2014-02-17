(ns incidents.geo
  (:require [clj-http.client :as client]
            [incidents.db :as db]
            [taoensso.timbre :as log]
            [environ.core :as env]))

(defonce running-update (atom nil))

(defn handle-geo-error
  [{:keys [status] :as body}]
  (log/error body)
  (when (= "OVER_QUERY_LIMIT" status)
    ;; might as well stop. Log it in a different future because this one goes away!
    (future (log/error "Cancelling geo job, google has said cut it out."))
    (future-cancel @running-update)))

(defn geocode-address
  [addr]
  ;; Doing the sleep here, so that the doseq can blast through dead records quickly
  (Thread/sleep (:geo-rate-limit-sleep-ms env/env))
  (log/debug "Fetching from google: " addr)
  (try
    (let [{{:keys [error_message results] :as body} :body}
          (client/get (:geocoding-url env/env)
                      {:query-params {:address addr, :sensor false}
                       :as :json})]
      (if error_message 
        (handle-geo-error body)
        (first results))) ;; Most likely only really want the first result anyway.
    (catch Exception e
      (log/error e addr))))

(defn find-address
  [s]
  (when-let [match (-> #".*?(at|on) (.*Pacifica).*"
                       (re-matches s)
                       (nth 2))]
    ;; the Pacifica needs to be there so that it doesn't pull
    ;; up Monterey road in Monterey, for example.
    (str match ", CA")))


(defn fetch-address
  "Useful for doing counts and such"
  [item]
  (some-> item :description find-address))

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
  that doesn't already have a geo.
  Shuffles them as a dirty hack to get around persistently bad addresses"
  []
  (doseq [id (-> @db/db keys shuffle)
          :when (and (-> id nil? not) ;; there's one bad id in there
                     (->> id (get @db/db) :geo empty?))]
    (log/debug "adding geo for " id)
    (swap! db/db  (update-geo id)))
  (db/save-data!))


(defn start-geocoding
  []
  (when (and (future? @running-update)
             (->  @running-update future-done? not))
    (future-cancel @running-update))
  (reset! running-update (future (update-geos!))))

(comment
  ;; do it in a separate thread, which is killable.
  (start-geocoding)

  ;; to stop it
  (future-cancel @running-update)

  (future-done? @running-update)
  
  ;; how many so far
  (->> @db/db
       vals
       (filter :geo)
       count)

  ;; example one
  (->> "120814042"
       (get @db/db)
       :description
       get-geo)
  
  )







