(ns wire-report.error
  (:require [clojure.string :as string]
            [cljs.stacktrace :as stacktrace]))

(defn includes-any?
  [st bad-strings]
  (loop [bad-strings bad-strings]
    (if-let [bad (first bad-strings)]
      (or (string/includes? st bad)
          (recur (rest bad-strings)))
      false)))

(defn _demunge [munged]
  (when munged
    (->  munged
         (string/replace-first  #"\.cljs\$lang\$test" "")
         string/trim
         demunge
         symbol)))

(def ^:dynamic *ua-product* :chrome)

(defn err->map
  "convert a js-error into a more ergonomic map
   opts:
     :exclude-files coll<string>
       - strings used to restrict collected stack lines

   The point of the user-stack is to remove noise in a raw error stack by providing
   a cutoff point via a string/includes? test. For example, when a test has an
   uncaught error, you can safely assume that the error occured in YOUR code,
   not the cljs.test harness. So all stacklines that are cljs.test and deeper
   are automatically removed (cljs/test.js is built-in).

   From here the :incident is the deepest point in the user-stack that passes
   the file-exclusions. This may or may not be what you want but its a start.

    {:message 'same as js-error'
     :type :TypeError
     :incident -> deepest user-stack map,
        {:file 'pathto/file.js'
         :column number
         :line number
         :function 'string$from$raw_trace'
         :var 'demunged.function/sy
            - may not exist & my not be correct if it does
            - cljs.core/demunge broken, see https://dev.clojure.org/jira/browse/CLJS-1726
              - maybe use devtools.munging/break-into-parts, use as symbols to lookup in cache?}
     :user-stack [{:function :column :line :file :var} {..} {..} ...]
       - includes :incident in last spot
     :raw js/Error}"
  [err &{:keys [exclude-files] :exclude-files nil}]
  (assert (instance? js/Error err))
  (let [stack (stacktrace/parse-stacktrace {} (.-stack err) {:ua-product *ua-product*} {:asset-root ""})
        exclude-files (into ["cljs/test.js"] exclude-files)
        user-stack (into []
                         (comp
                          (take-while
                           (fn [{file :file :as m}]
                             (not (includes-any? file exclude-files))))
                          (map
                           (fn [{f :function :as m}]
                             (assoc m :var (_demunge f)))))
                         (rest stack))]
    {:message (.-message err)
     :type (keyword (.-name err))
     :incident (last user-stack)
     :user-stack user-stack
     :raw err}))