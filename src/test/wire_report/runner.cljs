(ns wire-report.runner
  (:require 
   [cljs.test :as test :refer-macros [is deftest run-tests test-ns testing use-fixtures]]
   [cljs.nodejs :as nodejs]
   [cljs.pprint :refer [pprint]]
   [wire-report.core :as wire]))

(nodejs/enable-util-print!)

(deftest foo 
  (is (= 4 (+ 2 2)))
  (is (= 5 (+ 2 2)))
  (throw (js/Error. "an error")))

(deftest bar
  (testing "bar tests"
    (is (= (+ 2 2) 5))))

(defn baz [])

(use-fixtures :once {:before #() :after baz})

(def test-session (atom []))

(defn -main []
  (with-redefs
    [wire/send (fn [msg] (reset! test-session msg))]
    (run-tests {:reporter :wire})
    (pprint @test-session)))

(set! *main-cli-fn* -main)