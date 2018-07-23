(ns wire-report.error-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [clojure.string :as string]
            [wire-report.error :as E]
            [cljs.stacktrace :as stacktrace]))

(deftest err->map-test
  (testing "first stack line"
    (try
      (bar)
      (catch js/Error e
        (let [m (E/err->map e)]
        ; (is (= (:var m) 'wire-report.error-test/err->map-test))
          (is (= (:type m) :TypeError))
          (is (= 1 (count (get m :user-stack))))
          (is (= (:message m) "Cannot read property 'call' of undefined"))
          (is (string/ends-with? (get-in m [:incident :file]) "wire_report/error_test.js"))))))
  (testing "indirect"
    (try
      (string/ends-with? nil "55")
      (catch js/Error e
        (let [m (E/err->map e)]
        ;; (is (= (:var m) 'wire-report.error-test/err->map-test))
          (is (= (:type m) :TypeError))
          (is (= 3 (count (get m :user-stack))))
          (is (= (:message m) "Cannot read property 'length' of null"))
          (is (string/ends-with? (get-in m [:incident :file]) "wire_report/error_test.js"))))))
  (testing "same but filter this namespace"
    (try
      (string/ends-with? nil "55")
      (catch js/Error e
        (let [m (E/err->map e :exclude-files ["wire_report/error_test.js"])]
        ; (is (= (:var m) 'clojure.string/ends-with?))
          (is (= (:type m) :TypeError))
          (is (= 2 (count (get m :user-stack))))
          (is (= (:message m) "Cannot read property 'length' of null"))
          (is (string/ends-with? (get-in m [:incident :file]) "string.js")))))))

