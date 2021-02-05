(defproject incidents "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [
                 ;; [com.cemerick/friend "0.2.0"] ;; gah, not yet.
                 ;; [dire "0.5.2"] ; not actually used yet
                 [alandipert/enduro "1.2.0"]
                 [camel-snake-kebab "0.4.2"]
                 [cheshire "5.10.0"]
                 [clj-commons/conch "0.9.2"]
                 [clj-http "3.12.1"]
                 [clj-time "0.15.2"]
                 [com.cognitect.aws/api "0.8.498" :exclusions [org.eclipse.jetty/jetty-http
                                                               org.eclipse.jetty/jetty-io
                                                               org.eclipse.jetty/jetty-util]]
                 [com.cognitect.aws/s3 "810.2.817.0"]
                 [com.cognitect.aws/textract "809.2.797.0"]
                 [com.taoensso/timbre "4.10.0"] ;; NEVER UPGRRADE TO VERSION 5 IT IS BROKEN
                 [compojure "1.6.2" :exclusions [ring/ring-core]]
                 [enlive "1.1.6"]
                 [org.clojure/tools.reader "1.3.5"]
                 [environ "1.2.0"]
                 [firealarm "0.1.2" :exclusions [clj-stacktrace]]
                 [markdown-clj "1.10.5"]
                 [me.raynes/fs "1.4.6"]
                 [org.parkerici/multitool "0.0.11"] ;TODO public release for ken
                 [utilza "0.1.105" :exclusions [org.clojure/clojure]]
                 [org.clojure/clojure "1.10.2"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.clojure/tools.trace "0.7.10"] ;; required for dev
                 [ring "1.9.0"]
                 [ring.middleware.jsonp "0.1.6"]
                 [ring/ring-devel "1.9.0"]]
  :plugins [[lein-environ "1.2.0"]
            [lein-ring "0.12.5"]]
  :ring {:handler incidents.server/app
         :init    incidents.server/start
         }
  :main ^:skip-aot incidents.core
  :uberjar-name "incidents-standalone.jar"
  ;; Necessary to make the api docs work on heroku with uberjar WAR files.
  :filespecs [{:type :bytes :path "doc/API.md"
               :bytes ~(slurp "doc/API.md")}]
  ;; Might as well do this as soon as the project loads, for convenience.
  :profiles {;; :uberjar {:aot :all}
             :dev {:jvm-opts ["-XX:-OmitStackTraceInFastThrow"]}
             ;; mt: I don't like this, if there's a bug you get screwed by cider
             #_ :repl #_ {:injections [(do (require 'incidents.core)
                                     (incidents.core/-main)
                                     )]}
             }
  ;; defaults, you can overidde in .lein-env, or java environment
  :env {:dl-index-url "https://www.cityofpacifica.org/depts/police/media/media_bulletin.asp"
        :geocoding-url "http://maps.googleapis.com/maps/api/geocode/json"
        :db-filename "/tmp/incidents.db"
        :geo-rate-limit-sleep-ms 1000 ;; for google
        :port 8000 ;; default webserver port
        :timbre-config {:appenders {:spit {:enabled? true
                                           :fmt-output-opts {:nofonts? true}}
                                    :standard-out {:enabled? true
                                                   ;; nrepl/cider/emacs hates the bash escapes.
                                                   :fmt-output-opts {:nofonts? true}}}
                        ;; TODO: should only be in dev profile/mode
                        :shared-appender-config {:spit-filename "/tmp/wtf.log"}}})

