(ns incidents.geo
  (:require [clj-http.client :as client]
            [incidents.db :as db]
            [taoensso.timbre :as log]
            [environ.core :as env]))

(defonce running-update (atom nil))

(defonce enable-google? (atom true))

(defn handle-geo-error
  [{:keys [status] :as body}]
  (log/error body)
  (when (= "OVER_QUERY_LIMIT" status)
    ;; might as well stop. Log it in a different future because this one goes away!
    (log/error "Cancelling geo job, google has said cut it out.")
    (reset! enable-google? false)))

(defn geocode-address
  [addr]
  (when (and addr @enable-google?)
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
          (some-> results
                  first  ; Most likely only really want the first result anyway.
                  :geometry
                  :location)))
      (catch Exception e
        (log/error e addr)))))

(defn find-address
  [s]
  ;; the Pacifica needs to be there so that it doesn't pull
  ;; up Monterey road in Monterey, for example.
  (when-let [match (-> #".*?(at|on) (.*Pacifica).*"
                       (re-matches s)
                       (nth 2))]
    (str match ", CA")))



(defn ensure-address
  "Makes damn sure there's an address in there."
  [{:keys [address description] :as item}]
  (if address
    item
    (assoc item :address (find-address description))))

(defn find-existing-geo
  "Looks for dupes already in db"
  [addr]
  (when addr
    (some->> @db/db
             vals
             (filter #(= (:address %) addr))
             (filter #(some-> % :geo empty? not))
             first
             :geo)))

(defn copy-or-fetch-geo
  [{:keys [address] :as item}]
  (assoc item :geo (or (find-existing-geo address)
                       (geocode-address address))))

(defn add-geo-and-address
  [item]
  (->> item
       ensure-address
       copy-or-fetch-geo))




(defn update-geos!
  "Geocode everything in the db
  that doesn't already have a geo.
  Shuffles them as a dirty hack to get around persistently bad addresses"
  []
  (doseq [id (-> @db/db keys shuffle)
          :when (and id
                     (some-> id nil? not) ;; there's one bad id in there
                     (some->> id (get @db/db) :geo nil? not))]
    (log/debug "adding geo for " id)
    (swap! db/db  (db/update-record id add-geo-and-address)))
  (db/save-data!))


(defn start-geocoding
  []
  (reset! enable-google? true)
  (when (and (future? @running-update)
             (->  @running-update future-done? not))
    (future-cancel @running-update))
  (reset! running-update (future (try
                                   (update-geos!)
                                   (catch Exception e
                                     (log/error e))
                                   (finally
                                     (db/save-data!))))))

(comment
  ;; do it in a separate thread, which is killable.
  (start-geocoding)

  ;; to stop it
  (future-cancel @running-update)

  (reset! enable-google? false)
  
  (future-done? @running-update)
  
  ;; how many so far
  (->> @db/db
       vals
       (filter :geo)
       count)

  ;; example one
  (->> 130927231
       (get @db/db))




  
  )







