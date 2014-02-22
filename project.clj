(defproject incidents "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.4.4"]
                 [clj-http "0.7.9"]
                 ;; [com.cemerick/friend "0.2.0"] ;; gah, not yet.
                 [liberator "0.10.0"]
                 [firealarm "0.1.2"]
                 [ring.middleware.jsonp "0.1.3"]
                 [compojure "1.1.5"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [ring/ring-devel "1.2.0"]
                 [instaparse "1.2.14"]
                 [markdown-clj "0.9.41"]
                 [enlive "1.1.5"]
                 ;; [dire "0.5.2"] ; not actually used yet
                 [org.apache.pdfbox/pdfbox "1.8.4"]
                 [environ "0.4.0"]
                 [com.taoensso/timbre "3.0.1"]
                 [utilza "0.1.49" :exclusions [org.clojure/clojure]]]
  :plugins [[lein-environ "0.4.0"]
            [lein-ring "0.8.7"]]
  :ring {:handler incidents.server/app
         :init    incidents.server/start
         }
  ;; Might as well do this as soon as the project loads, for convenience.
  :injections [(do (require 'incidents.core)
                   (incidents.core/-main)
                   )]
  ;; defaults, you can overidde in .lein-env, or java environment
  :env {:dl-index-url "http://www.pacificaindex.com/policelogs.html"
        :geocoding-url "http://maps.googleapis.com/maps/api/geocode/json"
        :db-filename "/tmp/incidents.db"
        :geo-rate-limit-sleep-ms 1000 ;; for google
        :timbre-config {:appenders {:spit {:enabled? true
                                           :fmt-output-opts {:nofonts? true}}
                                    :standard-out {:enabled? true
                                                   ;; nrepl/cider/emacs hates the bash escapes.
                                                   :fmt-output-opts {:nofonts? true}}}
                        :shared-appender-config {:spit-filename "/tmp/wtf.log"}}})

