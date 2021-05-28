(ns incidents.core
  (:require [incidents.geo :as geo]
            [incidents.db :as db]
            [incidents.server :as srv]
            [taoensso.timbre :as log]
            [environ.core :as env]
            [clojure.tools.trace :as trace]
            [utilza.repl :as urepl]
            )
  (:gen-class))


;; IMPORTANT: This bare exec is here to dothis FIRST before running anything, at compile time
#_(log/merge-config! (:timbre-config env/env)) ;; XXX fails to compile anymore



;; IMPORTANT: enables the very very awesome use of clojure.tools.trace/trace-vars , etc
;; and logs the output of those traces to whatever is configured for timbre at the moment!
(alter-var-root #'clojure.tools.trace/tracer (fn [_]
                                               (fn [name value]
                                                 (log/debug name value))))


(defn -main
  []
  (future
    (try
      (let [{:keys [port]} env/env]
        (log/info (format "starting web server on port %s" port))
        (srv/start port))
      (log/info "web server started (presumbably)")
      (db/db-init)
      (catch Exception e
        (log/error e)))))








