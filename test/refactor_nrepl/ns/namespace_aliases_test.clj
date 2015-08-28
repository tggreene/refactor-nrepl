(ns refactor-nrepl.ns.namespace-aliases-test
  (:require [clojure.test :as t]
            [refactor-nrepl.ns
             [helpers :as util]
             [namespace-aliases :as sut]]
            [refactor-nrepl.util :as rutil]))

(t/deftest finds-the-aliases-in-this-ns
  (let [aliases (:clj (sut/namespace-aliases))]
    (t/is (some #(and (= (first %) 'sut)
                      (= (first (second %)) 'refactor-nrepl.ns.namespace-aliases))
                aliases))))

(t/deftest finds-the-cljs-alises-in-cljsns
  (let [aliases (:cljs (sut/namespace-aliases))]
    (t/is (some #(and (= (first %) 'pprint)
                      (= (first (second %)) 'cljs.pprint))
                aliases))))

(t/deftest sorts-by-frequencies
  (let [aliases (:clj (sut/namespace-aliases))
        _ (util/ns-form-from-string "(ns foo)")
        utils (get (rutil/filter-map #(= (first %) 'util) aliases) 'util)]
    (t/is (= (first utils) 'refactor-nrepl.util))
    (t/is (some #(= % 'refactor-nrepl.ns.helpers) utils))))
