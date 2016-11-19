(ns wire-report.core
  (:require [cljs.test :as test :refer-macros [run-tests test-ns]]
            [clojure.string :as string]
            [cognitect.transit :as transit]))

(def net (js/require "net"))

(defonce client (atom nil))

(defn shutdown-client []
  (when @client
    (.end @client)
    (.unref @client)))

(defn err->map [err]
  {:message (.-message err) :stack (.-stack err)})

(defn terr->map [err]
  [:transit-write-error
   (assoc (err->map err) :data (js/JSON.stringify (.-data err)))])

(defn write-transit [obj]
  {:post [string?]}
  (let [writer (transit/writer :json)] ;extensible handlers would be nice
    (try
      (transit/write writer obj)
      (catch js/Error e
        (let [writer (transit/writer :json)] ;must refresh writer
          (transit/write writer (terr->map e)))))))

(defn read-transit [obj]
  (let [reader (transit/reader :json)]
    (transit/read reader obj)))

(defn connected? [] (or @client (.-send js/process)))

(defn send-ipc [msg](js/process.send msg))

(defn send-sock [msg](.write @client msg #(shutdown-client)))

(defn send [summary]
  (let [msg (write-transit [(.cwd js/process) summary])]
    (if-not @client (send-ipc msg) (send-sock msg))))

(defn start-client [opts] (reset! client (net.connect opts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod test/report [:wire :begin-test-ns] [m] nil)

(defmethod test/report [:wire :pass] [m] (test/inc-report-counter! :pass))

(defn fn->name [f]
  (let [n (.-name f)]
    (if-not (empty? n) (demunge n) :anonymous-fn)))

(defn name->sym [s]
  (let [cs (string/split s "/")
        [dots f] (split-at (dec (count cs)) cs)
        st (str (string/join "." dots) "/" (first f) )]
    (symbol st)))

(defn fn->sym [f]
  (let [n (fn->name f)]
    (if (= :anonymous-fn n) n (name->sym n))))

(defn- process-fixtures
  "cannot serialize fns so just grabbing names"
  [fixtures] ; [{foo.bar.baz [{:before ... :after}]} ...]
  (into {}
    (for [[namesp fixes] fixtures
          {:keys [before after]} fixes]
      [namesp {:before (if before (fn->sym before))
               :after (if after (fn->sym after))}])))

(defn var->sym
  " #'cljs.foo/bar -> 'cljs.foo/bar"
  [v]
  (symbol (.replace (pr-str v) #"#'" "")))


(defn process-ctx [{:keys [testing-vars each-fixtures once-fixtures
                           testing-contexts] :as ctx}]
  (cond-> ctx
    testing-vars (assoc :testing-vars (var->sym (first testing-vars)))
    once-fixtures (assoc :once-fixtures (process-fixtures once-fixtures))
    each-fixtures (assoc :each-fixtures (process-fixtures each-fixtures))
    testing-contexts (assoc :testing-contexts (vec testing-contexts))))


(def fails (atom []))

(defmethod test/report [:wire :fail] [m]
  (let [m (dissoc m :type)
        ctx (-> (test/get-current-env)
              (dissoc :report-counters :reporter)
              process-ctx
              (assoc :actual (pr-str (:actual m)) :expected (pr-str (:expected m))))]
    (swap! fails conj  (merge m ctx))
    (test/inc-report-counter! :fail)))

(def errors (atom []))

(defmethod test/report [:wire :error] [m]
  (let [m (dissoc m :type)
        ctx (-> (test/get-current-env)
              (dissoc :report-counters :reporter)
              process-ctx
              (assoc :actual (err->map (:actual m))))]
    (swap! errors conj (merge m ctx))
    (test/inc-report-counter! :error)))

(defmethod test/report [:wire :summary] [m]
  (let [summ [:summary (assoc (dissoc m :type) :fails @fails :errors @errors)]]
    (send summ)))

