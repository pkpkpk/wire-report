# wire-report


Use either IPC or a network socket to send a test-report object (via transit) back to your controlling process.

Optionally pass a [`net.Socket` options object](https://nodejs.org/api/net.html#net_net_connect_options_connectlistener) to `wire/start-client`. The socket will take precedence over IPC and will automatically cleanup after itself once the report has been written.

Use `wire/connected?` to conditionally use the `:wire` reporter key. If socket creation was omitted,  it will automatically defer to `js/process.send`

```clojure
(let [client (if opts (wire/start-client (clj->js opts)))]
  (if (wire/connected?)
    (run-tests {:reporter :wire}
      'project.test.core
    (run-tests
      'project.test.core)))
```

<hr>

#### The report object

 The vanilla state collected by cljs.test cannot be serialized in situ, so there are some changes:
 
 1. var forms `#'foo.bar` are converted to simple symbols: `'foo.bar`
 +  fixture fns are replaced with their qualified symbols or labeled as `:anonymous-fn`
 + js/Error objects cannot be serialized: their message and stack are sent in simple maps
 + `:expected` & `:actual` forms are just strings
 + testing-contexts is a vector of strings instead of list
 
 
 ```clojure
[:summary
 {:fail 2,
  :error 1,
  :pass 1,
  :test 2,
  :fails
  [{:file "cljs/test.js",
    :line 432,
    :column 14,
    :expected "(= 5 (+ 2 2))",
    :actual "(not (= 5 4))",
    :message nil,
    :once-fixtures
    {wire-report.runner
     {:before :anonymous-fn, :after wire-report.runner/baz}},
    :testing-vars wire-report.runner/foo}
   {:testing-vars wire-report.runner/bar,
    :testing-contexts ["bar tests"],
    :file "cljs/test.js",
    :column 14,
    :line 432,
    :expected "(= (+ 2 2) 5)",
    :actual "(not (= 4 5))",
    :message nil,
    :once-fixtures
    {wire-report.runner
     {:before :anonymous-fn, :after wire-report.runner/baz}}}],
  :errors
  [{:file "Error",
    :line NaN,
    :column NaN,
    :message "Uncaught exception, not in assertion.",
    :expected nil,
    :actual
    {:message "an error",
     :stack
     "Error: an error\n    at wire_report.runner.foo.cljs$lang$test..."},
    :once-fixtures
    {wire-report.runner
     {:before :anonymous-fn, :after wire-report.runner/baz}},
    :testing-vars wire-report.runner/foo}]}]
 ```