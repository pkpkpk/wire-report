(ns wire-report.error
  (:require [clojure.string :as string]))

(defn mapped-line? [line] (.test #"\d\)$" line))

(defn stackline->var-info
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
    ; (util/err js-address)
    {:col col
     :row row
     :js-address js-address
     :js-file (subs js-address 0 (.-index (.exec #":\d" js-address)))
     :var var}))

(defn err->map [err]
  (let [msg (pr-str (.-message err))
        stack (string/split-lines (.-stack err))]
    (cond
      (string/includes? msg "property 'call'")
      (let [{:keys [col row js-file var]} (stackline->var-info (second stack))]
        {:type :bad-function-call
         :raw err
         :message msg
         :var var
         :js-file js-file
         :address {:row row :col col}})

      (string/includes? msg "Invalid arity:")
      (let [{:keys [col row js-file var]} (stackline->var-info (second stack))]
        {:type :invalid-arity
         :raw err
         :message msg
         :var var
         :js-file js-file
         :address {:row row :col col}})

      (string/includes? msg "is not a function")
      (let [{:keys [col row js-file var]} (stackline->var-info (second stack))]
        {:type :not-a-function
         :raw err
         :message msg
         :var var
         :js-file js-file
         :address {:row row :col col}})

      (and (string/includes? msg "Cannot read property")
           (or (string/includes? msg "of undefined")
               (string/includes? msg "of null")))
      (let [{:keys [col row js-file var]} (stackline->var-info (second stack))]
        {:type :undefined-type
         :suggestion :typo?
         :raw err
         :message msg
         :var var
         :js-file js-file
         :address {:row row :col col}})

      :else
      {:raw err
       :message msg
       :stack stack})))