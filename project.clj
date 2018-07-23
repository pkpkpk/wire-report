(defproject wire-report "0.2.0"
  :description "Send clojurescript node tests results over the wire"
  :url "https://github.com/pkpkpk/wire-report"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]
                 [com.cognitect/transit-cljs "0.8.256"]
                 [org.clojure/core.async "0.4.474"]]

  :source-paths ["src/main"])
