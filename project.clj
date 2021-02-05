(defproject incidents "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.2"]
                 [org.clojure/java.jdbc "0.7.11"]
                 [cheshire "5.10.0"]
                 [clj-time "0.4.4"]
                 [clj-http "3.11.0"]
                 ;; [com.cemerick/friend "0.2.0"] ;; gah, not yet.
                 [org.clojure/tools.trace "0.7.6"] ;; required for dev
                 [firealarm "0.1.2"]
                 [ring "1.8.2"]
                 [ring.middleware.jsonp "0.1.6"]
                 [compojure "1.6.2"]
                 [ring/ring-jetty-adapter "1.8.2"]
                 [ring/ring-devel "1.8.2"]
                 [instaparse "1.4.10"]
                 [markdown-clj "0.9.41"]
                 [enlive "1.1.5"]
                 ;; [dire "0.5.2"] ; not actually used yet
                 [alandipert/enduro "1.2.0"]
                 [environ "0.4.0"]
                 [com.taoensso/timbre "3.0.1"]
                 [me.raynes/fs "1.4.6"]
                 [org.parkerici/multitool "0.0.11"] ;TODO public release for ken
                 [utilza "0.1.49" :exclusions [org.clojure/clojure]]]
  :plugins [[lein-environ "0.4.0"]
            [lein-ring "0.8.7"]]
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

