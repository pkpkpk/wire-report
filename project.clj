(defproject wire-report "0.1.0"
  :description "Send clojurescript node tests results over the wire"
  :url "https://github.com/pkpkpk/wire-report"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.cognitect/transit-clj "0.8.288"]
                 [org.clojure/core.async "0.2.395"
                  :exclusions [org.clojure/tools.reader]]]

  :plugins [[lein-figwheel "0.5.4-7"]
            [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :compiler {:main wire-report.runner
                           :target :nodejs
                           :asset-path "target/out"
                           :output-to "target/wire_report.js"
                           :output-dir "target/out"
                           :source-map-timestamp true
                           :parallel-build true
                           :cache-analysis true}}]})
