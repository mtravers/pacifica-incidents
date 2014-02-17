(ns incidents.server
  (:require [ring.adapter.jetty :as jetty]
            [firealarm.core :as firealarm]
            ;;            [ring.middleware.jsonp :as jsonp]
            [compojure.handler :as handler]
            [incidents.api :as api]))


(defonce srv (atom nil))

(def wrap-exceptions
  (firealarm/exception-wrapper
   (firealarm/file-reporter "/tmp/web.log")))


(def app
  (-> #'api/routes
      ;;jsonp/wrap-json-with-padding
      handler/site
      wrap-exceptions))

(defn start
  []
  (future
    (swap! srv (fn [s]
                 (when (and s (.running s))
                   (.stop s))
                 ;; TODO: port in env/env
                 (jetty/run-jetty #'app {:port 8000})))))
