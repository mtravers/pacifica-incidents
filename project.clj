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
                 [utilza "0.1.49" :exclusions [org.clojure/clojure]]]
  :plugins [[lein-environ "0.4.0"]]
  ;; defaults, you can overidde in .lein-env, or java environment
  :env {:dl-url-format "http://www.pacificaindex.com/policelogs/PPDdailymediabulletin%s.pdf"
        :geocoding-url "http://maps.googleapis.com/maps/api/geocode/json"
        :db-filename "/tmp/incidents.db"})

