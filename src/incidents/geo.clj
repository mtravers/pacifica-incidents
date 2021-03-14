(ns incidents.geo
  (:require [clj-http.client :as client]
            [incidents.db :as db]
            [taoensso.timbre :as log]
            [org.parkerici.multitool.core :as u]
            [clojure.string :as s]
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

(defn geocode-address-1
  [addr]
  (try
    (let [{{:keys [error_message results] :as body} :body}
          (client/get (:geocoding-url env/env)
                      {:query-params {:key (:gmap-api-key env/env)
                                      :address addr
                                      :sensor false}
                       :as :json})]
      (if error_message 
        (throw (ex-info "Geo error" {:addr addr :error error_message})) ;; (handle-geo-error body)
        (some-> results
                first  ; Most likely only really want the first result anyway.
                :geometry
                :location)))
    (catch Exception e
      (log/error e addr))))

(u/defn-memoized geocode-address
  [addr]
  (when (and addr @enable-google?)
    ;; Doing the sleep here, so that the doseq can blast through dead records quickly
    (Thread/sleep (u/coerce-numeric (:geo-rate-limit-sleep-ms env/env))) ;For some reason this is getting stringified
    (log/debug "Fetching from google: " addr)
    (geocode-address-1 addr)))

(defn clean-address
  "Turn incidents location desc into valid address"
  [s]
  (and s (str (s/replace s #"/" " & ") ", Pacifica CA")))

(defn ensure-address
  "Makes damn sure there's an address in there."
  [{:keys [address location] :as item}]
  (if address
    item
    (assoc item :address (clean-address location))))

(defn add-geo
  [{:keys [address] :as item}]
  (if address
    (assoc item :geo (geocode-address address))
    item))

(defn ensure-geo
  "Takes an item map. Returns the item map with geocode data and address inserted."
  [item]
  {:pre [(map? item)]}
  (some->> item
           ensure-address
           add-geo
           ))

(defn update-geos!
  "Geocode everything in the db
  that doesn't already have a geo.
  Shuffles them as a dirty hack to get around persistently bad addresses"
  []
  (db/update-files!
   (fn [file]
     (update file :entries #(map ensure-geo %)))))


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
  




  
  )







