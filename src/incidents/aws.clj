(ns incidents.aws
  (:require [incidents.db :as db]
            [incidents.utils :as utils]
            [cognitect.aws.credentials :as credentials]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [clojure.walk :as walk]
            [utilza.log :as ulog]
            [utilza.aws :as uaws]
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
