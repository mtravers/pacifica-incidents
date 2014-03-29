(ns incidents.server
  (:require [ring.adapter.jetty :as jetty]
            [firealarm.core :as firealarm]
            [ring.middleware.jsonp :as jsonp]
            [ring.middleware.resource :as res]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [taoensso.timbre :as log]
            [environ.core :as env]
            [incidents.api :as api]
            [incidents.db :as db]))


(defonce srv (agent nil))

(def wrap-exceptions
  (firealarm/exception-wrapper
   (firealarm/file-reporter "/tmp/web.log")))

(compojure/defroutes routes
  (compojure/GET "/" {}
                 (ring.util.response/redirect "map.html"))
  (compojure/GET "" {}
                 (ring.util.response/redirect "map.html"))
  (compojure/GET "/index.html" {}
                 (ring.util.response/redirect "map.html"))
  (compojure/context "/api" [] api/routes))

(def app
  (-> routes
      (res/wrap-resource "public")
      jsonp/wrap-json-with-padding
      handler/site
      wrap-exceptions
      ))

(defn coerce-to-number [x]
  (if (number? x) x (read-string x)))

(defn start [& port]
  (let [port (coerce-to-number (or (first port) (env/env :port)))]
    (println (list 'port port))
  (send srv
        (fn [s]
          (when (and s (.isRunning s))
            (.stop s))
          ;; TODO: port in env/env
          (log/info "Starting server")
          (jetty/run-jetty #'app {:port port
                                  :join? false})))))


(comment

  (start 8000)


  )
