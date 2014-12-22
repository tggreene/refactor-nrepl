(ns refactor-nrepl.test-namespace
  (:require [clojure.test :refer :all]
            [refactor-nrepl.ns
             [clean-ns :refer [clean-ns read-ns-form]]
             [helpers :refer [get-ns-component]]]
            [refactor-nrepl.ns.pprint :refer [pprint-ns]])
  (:import java.io.File))

(def ns1 (.getAbsolutePath (File. "test/resources/ns1.clj")))
(def ns1-cleaned (read-ns-form (.getAbsolutePath (File. "test/resources/ns1_cleaned.clj"))))
(def ns2 (.getAbsolutePath (File. "test/resources/ns2.clj")))
(def ns2-cleaned (read-ns-form (.getAbsolutePath (File. "test/resources/ns2_cleaned.clj"))))
(def ns-with-exclude (read-ns-form (.getAbsolutePath (File. "test/resources/ns_with_exclude.clj"))))
(def ns-with-unused-deps (.getAbsolutePath (File. "test/resources/unused_deps.clj")))
(def ns-without-unused-deps (read-ns-form
                             (.getAbsolutePath (File. "test/resources/unused_removed.clj"))))
(deftest combines-requires
  (let [requires (get-ns-component (clean-ns ns2) :require)
        combined-requires (get-ns-component ns2-cleaned :require)]
    (is (= combined-requires requires))))

(deftest preserves-removed-use
  (let [requires (get-ns-component (clean-ns ns2) :use)
        combined-requires (get-ns-component ns2-cleaned :require)]
    (is (reduce
         #(or %1 (= %2 '[clojure
                         [edn :refer :all :rename {read-string rs read rd}]
                         [instant :refer :all]
                         [pprint :refer [cl-format fresh-line get-pretty-writer]]
                         [string :refer :all :rename {replace foo reverse bar}]
                         [test :refer :all]]))
         false
         (tree-seq sequential? identity combined-requires)))))

(deftest removes-use-with-rename-clause
  (let [requires (get-ns-component (clean-ns ns2) :use)
        combined-requires (get-ns-component ns2-cleaned :require)]
    (is (reduce
         #(or %1 (= %2 '[edn :refer :all :rename {read-string rs
                                                  read rd}]))
         false
         (tree-seq sequential? identity combined-requires)))))

(deftest test-sort-and-prefix-favoring
  (let [requires (get-ns-component (clean-ns ns1) :require)
        imports (get-ns-component (clean-ns ns1) :import)
        sorted-requires (get-ns-component ns1-cleaned :require)
        sorted-imports (get-ns-component ns1-cleaned :import)]
    (is (= sorted-requires requires))
    (is (= sorted-imports imports))))

(deftest throws-exceptions-for-unexpected-elements
  (is (thrown? IllegalArgumentException
               (clean-ns ns-with-exclude))))

(deftest throws-on-malformed-ns
  (is (thrown? IllegalArgumentException
               (read-ns-form (.getAbsolutePath
                              (File. "test/resources/clojars-artifacts.edn"))))))

(deftest preserves-other-elements
  (let [actual (clean-ns ns1)
        docstring (nthrest actual 2)
        author (nthrest actual 3)
        refer-clojure (nthrest actual 4)
        gen-class (nthrest actual 5)]
    (is (= (nthrest ns1-cleaned 2) docstring))
    (is (= (nthrest ns1-cleaned 3) author))
    (is (= (nthrest ns1-cleaned 4) refer-clojure))
    (is (= (nthrest ns1-cleaned 5) gen-class))))

(deftest removes-use
  (let [use-clause (get-ns-component ns1-cleaned :use)]
    (is (nil? use-clause))))

(deftest combines-multiple-refers
  (let [requires (clean-ns ns2)
        refers '[cl-format fresh-line get-pretty-writer]]
    (is (reduce
         #(or %1 (= %2 refers))
         false
         (tree-seq sequential? identity requires)))))

(deftest combines-multiple-refers-to-all
  (let [requires (clean-ns ns2)
        instant '[instant :refer :all]]
    (is (reduce
         #(or %1 (= %2 instant))
         false
         (tree-seq sequential? identity requires)))))

(deftest removes-unused-dependencies
  (let [new-ns (clean-ns ns-with-unused-deps)
        requires (get-ns-component new-ns :require)
        imports (get-ns-component new-ns :import)
        clean-requires (get-ns-component ns-without-unused-deps :require)
        clean-imports (get-ns-component ns-without-unused-deps :import)]
    (is (= clean-requires requires))
    (is (= clean-imports imports))))