(ns wire-report.core
  (:require [cljs.test]))


(defmacro run-tests
  [& args]
  `(if (~'wire-report.core/connected?)
     (cljs.test/run-tests {:reporter :wire} ~@args)
     (cljs.test/run-tests ~@args)))