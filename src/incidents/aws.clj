(ns incidents.aws
  (:require [incidents.utils :as utils]
            [cognitect.aws.credentials :as credentials]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [clojure.walk :as walk]
            [utilza.log :as ulog]
            [utilza.aws :as uaws]
            [me.raynes.fs :as fs]
            [org.parkerici.multitool.core :as u]
            [org.parkerici.multitool.cljcore :as ju]
            [cognitect.aws.client.api :as aws]
            ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment

  (def services (uaws/make-services [:s3 :textract]
                                    {:credentials-provider (credentials/profile-credentials-provider "restivo")}))


  (ulog/spewer
   (uaws/list-ops services :textract))

  (ulog/spewer
   (uaws/docs services :textract :AnalyzeDocument))

  )

;;; TODO this doesn't show much detail about error 
(defn invoke-with-error
  [service op]
  (let [result (aws/invoke service op)]
    (when (or (:Error result)
              (:cognitect.anomalies/category result))
      (throw (ex-info "AWS error" {:op op :aws-result result})))
    result))

(defn invoke-paged
  "Takes a service, a map of options to pass to invoke, and a key to find the actual results.
  Calls invoke with those args on an AWS function.
  Pages through all the results.
  Returns the aggregate results, which will be result-key"
  [service invoke-options result-key]
  (loop [acc {}]
    (print ".")
    (let [res (invoke-with-error service (if (-> acc :NextToken empty?)
                                           invoke-options
                                           (assoc-in invoke-options [:request :NextToken] (:NextToken acc))))
          new-acc (-> acc
                      (update-in [result-key] concat (result-key res))
                      (assoc :NextToken (:NextToken res)))]
      (when (:ErrorResponse res)
        (throw (ex-info "AWS Call Failed" res)))
      (if (-> new-acc :NextToken empty?)
        (result-key new-acc)
        (recur new-acc)))))

;;; Fixed version of fn from utiliza
(defn invoke-paged
  "Takes a service, a map of options to pass to invoke, and a key to find the actual results.
  Calls invoke with those args on an AWS function.
  Pages through all the results.
  Returns the aggregate results, which will be result-key"
  [service invoke-options result-key]
  (loop [acc []
         options invoke-options]
    (let [res (invoke-with-error service options)
          new-acc (concat acc (result-key res))]
      (when (:ErrorResponse res)
        (throw (ex-info "AWS Call Failed" res)))
      (if (= (:JobStatus res) "SUCCEEDED") ; note: this may be specific to :textract api
        (if (-> res :NextToken empty?)
          new-acc
          (recur new-acc (assoc-in options [:request :NextToken] (:NextToken res))))
        ;; not ready
        (do
          (java.lang.Thread/sleep 1000)
          (recur acc options))))))


(u/defn-memoized client [service]
  (aws/client {:api service
               :region "us-west-2"
               :credentials-provider (credentials/profile-credentials-provider "incidents2")}))

(defn job-id->blocks
  [job-id]
  (invoke-paged
   (client :textract)
   {:op :GetDocumentAnalysis
    :request {:JobId job-id}}
   :Blocks))

(def bucket "pacifica-incidents")

(defn file->s3
  [file s3key]
  (with-open [stream (clojure.java.io/input-stream file)]
    (invoke-with-error (client :s3)
                       {:op :PutObject
                        :request {:Bucket bucket
                                  :Key s3key
                                  :Body stream}}))
  )

(defn spit-to-s3 [thing key]
  (let [local (fs/temp-name "spit")]
    (ju/schppit local thing)
    (file->s3 local key)
    key))

(defn s3->file
  [s3key file]
  (let [resp (invoke-with-error (client :s3)
                                {:op :GetObject
                                 :request {:Bucket bucket
                                           :Key s3key
                                           }})
        stream (:Body resp)]
    (with-open [w (clojure.java.io/writer file)]
      (clojure.java.io/copy stream w))))



;;; TODO start the job and wait for it to be ready
(defn parse-pdf-s3
  [key]
  (let [job-id (:JobId
                (invoke-with-error (client :textract)
                                   {:op :StartDocumentAnalysis
                                    :request {:DocumentLocation {:S3Object {:Bucket bucket
                                                                            :Name key}}
                                              :FeatureTypes ["TABLES"]}}))]
    (log/info "Starting Textract job" job-id)
    (job-id->blocks job-id)))

(defn parse-pdf-local
  [file]
  (let [key (ju/random-uuid)]
    (file->s3 file key)
    (parse-pdf-s3 key)))



