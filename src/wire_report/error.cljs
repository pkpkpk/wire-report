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

(defn user-stack
  "starting at top of error stack, assemble chain down to first call that
   is not a clojure/goog namespace" ;- make extensible
  [stack]
  (let [acc #js[]]
    (loop [stack (rest stack)]
      (when-let [line (first stack)]
        (when (not (string/includes? line "cljs/test.js"))
          (let [{v :var :as var-info} (stackline->var-info line)]
            (.push acc var-info)
            (recur (rest stack))))))
    (vec acc)))

(defn err->map [err]
  (assert (instance? js/Error err))
  (let [msg (pr-str (.-message err))
        stack (string/split-lines (.-stack err))
        user-stack (user-stack stack)
        incident (last user-stack)]
    {:message msg
     :type (keyword (.-name err))
     :var (get incident :var)
     :js-address (dissoc incident :var)
     ; :raw-error err
     ; :stack stack
     :user-stack user-stack
     :raw err}))