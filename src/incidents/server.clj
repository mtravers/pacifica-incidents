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
            [incidents.db :as db]
            [org.parkerici.multitool.core :as u]
            ))


(defonce srv (agent nil))

(def wrap-exceptions
  (firealarm/exception-wrapper
   (firealarm/file-reporter "/tmp/web.log")))

;;; → multitool
(defn expand-template-string
  "Template is a string containing {foo} elements, which get replaced by corresponding values from bindings"
  [template bindings]
  (let [matches (->> (re-seq #"\{(.*?)\}" template) ;extract the template fields from the entity
                     (map (fn [[match key]]
                            [match (or (bindings key)
                                       (bindings (keyword key))
                                       "")])))]
    (reduce (fn [s [match key]]
              (clojure.string/replace s (u/re-pattern-literal match) (str key)))
            template matches)))

(u/def-lazy map-page-content
  (expand-template-string
   (slurp "resources/public/map.html")
   env/env))

(defn map-static-page
  "Miserable hack. For some reason, file-response is botched on heroku"
  []
  ;; (resp/file-response "map.html" {:root "resources/public"})
  {:status 200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body @map-page-content})

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
