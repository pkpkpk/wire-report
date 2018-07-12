(ns wire-report.error-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [clojure.string :as string]
            [wire-report.error :as E]))

(deftest err->map-test
  (testing "calling undefined function"
    (try
      (bar)
      (catch js/Error e
        (let [m (E/err->map e)]
          (is (= (:var m) 'wire-report.error-test.err->map-test))
          (is (= (:type m) :TypeError))
          (is (string/includes? (:message m) "Cannot read property 'call' of undefined"))
          (is (string/ends-with? (get-in m [:js-address :file]) "wire_report/error_test.js")))))))

