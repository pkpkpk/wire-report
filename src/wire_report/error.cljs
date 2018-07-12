(ns wire-report.error
  (:require [clojure.string :as string]))

(defn mapped-line? [line] (.test #"\d\)$" line))

(defn stackline->var-info ; v8 specific?
  [line]
  (assert (string? line))
  (let [line (string/replace-first line  #"^ +at " "")
        [munged js-address] (if (mapped-line? line)
                              (string/split line #" \(")
                              [nil line])
        var (when munged
              (->  munged
                   (string/replace-first  #"\.cljs\$lang\$test" "")
                   string/trim
                   demunge
                   symbol))
        [row col] (into []
                      (comp
                       (remove empty?)
                       (map #(string/replace % #"\)" ""))
                       (map #(js/parseInt %)))
                      (rest (array-seq (.split (subs js-address i) ":"))))]
    {:col col
     :row row
     :stack-line line ;<= devtools treats as url
     :file (subs js-address 0 (.-index (.exec #":\d" js-address)))
     :var var}))

(defn includes-any?
  [st bad-strings]
  (loop [bad-strings bad-strings]
    (if-let [bad (first bad-strings)]
      (or (string/includes? st bad)
          (recur (rest bad-strings)))
      false)))

(defn user-stack
  "starting at top of error stack, collect all avail info from each stackline
   until it hits no-include string. By default it keeps everything up to any
   \"cljs/test.js\". Pass a coll of strings to narrow accepted stack lines.
   Each test is conducted on the raw stack line"
  ([stack] (user-stack stack nil))
  ([stack no-include]
   (when no-include
     (assert (and (seq no-include) (every? string? no-include))))
   (let [stack (if (vector? stack) stack (string/split-lines stack))
         no-include-strings (into ["cljs/test.js"] no-include)
         acc #js[]]
     (loop [stack (rest stack)]
       (when-let [line (first stack)]
         (let []
           (when (not (includes-any? line no-include-strings))
             (let [{v :var :as var-info} (stackline->var-info line)]
               (.push acc var-info)
               (recur (rest stack)))))))
     (vec acc))))

(defn err->map
  "convert a js-error into a more ergonomic map
   opts:
     :no-include coll<string>
       - strings used to restrict collected stack lines

   The point of the user-stack is to remove noise in a raw error stack by providing
   a cutoff point via a no-include string test. For example, in a test namespace,
   when a test has an uncaught error, you can safely assume that the error occured
   in YOUR code, not the cljs.test harness. So all stacklines that are part of cljs.test
   are automatically removed. From here the 'incident' is the deepest point in
   the user-stack that passes the no-include test. This may or not be what you want
   but its a start.

   ret:
    {:message 'same as js-error'
     :type :TypeError
     :var symbol-from-incident-line
       - may not exist
       - cljs.core/demunge broken, see https://dev.clojure.org/jira/browse/CLJS-1726
         - maybe use devtools.munging/break-into-parts, use as symbols to lookup in cache?
     :js-address {:file 'pathto/file.js'
                  :col number
                  :row number
                  :stack-line 'raw-stack-line'}
     :user-stack [{:var :col :row :file} {..} {..} ...]
     :raw js/Error}"
  [err &{:keys [no-include] :no-include nil}]
  (assert (instance? js/Error err))
  (let [user-stack (user-stack (.-stack err) no-include)
        incident (last user-stack)]
    {:message (.-message err)
     :type (keyword (.-name err))
     :var (get incident :var)
     :js-address (dissoc incident :var)
     :user-stack user-stack
     :raw err}))