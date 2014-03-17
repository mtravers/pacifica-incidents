(ns incidents.core
  (:require [incidents.geo :as geo]
            [incidents.parse :as parse]
            [incidents.db :as db]
            [incidents.server :as srv]
            [taoensso.timbre :as log]
            [environ.core :as env]
            [clojure.tools.trace :as trace]
            [utilza.repl :as urepl]
            [incidents.dl :as dl])
  (:gen-class))

;; IMPORTANT: This bare exec is here to dothis FIRST before running anything, at compile time
(log/merge-config! (:timbre-config env/env))


;; IMPORTANT: enables the very very awesome use of clojure.tools.trace/trace-vars , etc
;; and logs the output of those traces to whatever is configured for timbre at the moment!
(alter-var-root #'clojure.tools.trace/tracer (fn [_]
                                               (fn [name value]
                                                 (log/debug name value))))


(defn -main
  []
  (future
    (try
      (db/db-init)
      (let [{:keys [port]} env/env]
        (log/info (format "starting web server on port %s" port))
        (srv/start port))
      (log/info "web server started (presumbably)")
      (catch Exception e
        (log/error e)))))


(comment

  ;; cron job 1:
  (scrape/start-pdf-downloading)


  ;; cron job 2
  (geo/start-geocoding)

  
  (-main)

  )





(comment
  ;; attempt to parse everthang

  ;; TODO: Do this as part of the downloading operation
  
  (count @db/db)

  (db/read-data!)
  
  (log/info "wtf?")
  
  )




