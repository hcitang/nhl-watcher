(defproject nhl-watcher-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
    [org.clojure/data.json "0.2.6"]
    [clj-http "3.7.0"]
    [clojure-lanterna "0.9.7"]
    ;[clojure.java-time "0.3.0"]
    [clj-time "0.14.2"]
    ]
  :main ^:skip-aot nhl-watcher-app.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
