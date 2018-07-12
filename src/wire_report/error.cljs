(ns wire-report.error
  (:require [clojure.string :as string]))

(defn mapped-line? [line] (.test #"\d\)$" line))

(defn stackline->var-info ; v8 specific?
  [line]
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
     :raw js-address ;<= devtools treats as url
     :file (subs js-address 0 (.-index (.exec #":\d" js-address)))
     :var var}))

(defn classify-msg [msg] ;=> ?map
  (cond
    (string/includes? msg "property 'call'")
    {:type :bad-function-call}

    (string/includes? msg "Invalid arity:")
    {:type :invalid-arity}

    (string/includes? msg "is not a function")
    {:type :not-a-function}

    (and (string/includes? msg "Cannot read property")
         (or (string/includes? msg "of undefined")
             (string/includes? msg "of null")))
    {:type :undefined-type
     :suggestion :typo?}

    :else {}))

(defn err->map [err]
  (let [msg (pr-str (.-message err))
        stack (string/split-lines (.-stack err))
        var-info (stackline->var-info (second stack))
        common {:message msg
                :var (get var-info :var)
                :js-address (dissoc var-info :var)
                :raw-error err
                :stack stack}]
    (merge common (classify-msg msg))))