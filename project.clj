(defproject incidents "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-time "0.4.4"]
                 [clj-http "0.7.9"]
                 [instaparse "1.2.14"]
                 [environ "0.4.0"]
                 [com.taoensso/timbre "3.0.1"]
                 [utilza "0.1.49" :exclusions [org.clojure/clojure]]]
  :plugins [[lein-environ "0.4.0"]]
  ;; Might as well do this as soon as the project loads, for convenience.
  :injections [(do (require 'incidents.core)
                   (incidents.core/-main))]
  ;; defaults, you can overidde in .lein-env, or java environment
  :env {:dl-url-format "http://www.pacificaindex.com/policelogs/PPDdailymediabulletin%s.pdf"
        :geocoding-url "http://maps.googleapis.com/maps/api/geocode/json"
        :db-filename "/tmp/incidents.db"
        :timbre-config {:appenders {:spit {:enabled? true
                                           :fmt-output-opts {:nofonts? true}}
                                    :standard-out {:enabled? true
                                                   :fmt-output-opts {:nofonts? true}}}
                        :shared-appender-config {:spit-filename "/tmp/wtf.log"}}})

