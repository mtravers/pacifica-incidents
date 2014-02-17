(ns incidents.api
  (:require [liberator.core :as liberator]
            [incidents.db :as db]
            [taoensso.timbre :as log]
            [clojure.walk :as walk]
            [compojure.core :as compojure])
  (:import java.util.Date))


(defn serialize-for-json
  [t]
  (walk/postwalk #(if (= java.util.Date (class %))
                    (.getTime %)
                    %)
                 t))


(defn- with-count
  [count xs]
  (cond->> xs count (take (Integer/parseInt count))))

(defn get-all [{:keys [count]}]
  (->> @db/db
       vals
       (sort-by :time)
       reverse
       (with-count count)
       serialize-for-json))


(liberator/defresource incidents
  :method-allowed? (liberator/request-method-in :get)
  
  :available-media-types ["application/json"
                          ;; application/clojure ;; could support edn, but why really?
                          ]
  :see-other (fn [context]
               (:new-url context))
  :handle-ok (fn [{{:keys [params]} :request}]
               (log/debug params)
               (get-all params)))


(compojure/defroutes routes
  (compojure/ANY "/incidents" [] incidents) ;; depreciated
  (compojure/ANY "/api" [] incidents))


(comment




  

  )