(ns incidents.server
  (:require [ring.adapter.jetty :as jetty]
            [firealarm.core :as firealarm]
            [ring.middleware.jsonp :as jsonp]
            [ring.middleware.resource :as res]
            [ring.util.response :as resp]
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

(defn map-static-page
  "Miserable hack. For some reason, file-response is botched on heroku"
  []
  ;; (resp/file-response "map.html" {:root "resources/public"})
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body (slurp "resources/public/map.html")})

(compojure/defroutes routes
  (compojure/GET "/" [] (map-static-page))
  (route/resources "/")
  (compojure/context "/api" [] #'api/routes)
  (route/not-found (resp/not-found "Not found")))

(def app
  (-> #'routes
      jsonp/wrap-json-with-padding ;; is this needed anymore?
      handler/site
      wrap-exceptions
      ))

(defn coerce-to-number [x]
  (if (number? x) x (clojure.edn/read-string x)))

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
