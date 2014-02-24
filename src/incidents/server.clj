(ns incidents.server
  (:require [ring.adapter.jetty :as jetty]
            [firealarm.core :as firealarm]
            [ring.middleware.jsonp :as jsonp]
            [ring.middleware.resource :as res]
            [compojure.handler :as handler]
            [incidents.api :as api]
            [incidents.db :as db]))


(defonce srv (agent nil))

(def wrap-exceptions
  (firealarm/exception-wrapper
   (firealarm/file-reporter "/tmp/web.log")))

(def app
  (-> #'api/routes
      (res/wrap-resource  "public")
      jsonp/wrap-json-with-padding
      handler/site
      wrap-exceptions))

(defn start [port]
  (db/db-init)
  (send srv
        (fn [s]
          (when (and s (.isRunning s))
            (.stop s))
          ;; TODO: port in env/env
          (jetty/run-jetty #'app {:port port, :join? false}))))


(comment

  (start)


  )
