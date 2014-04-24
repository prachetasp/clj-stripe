;   Copyright (c) 2014 Alberto Bengoa/Prachetas Prabhu. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clj-stripe.test.http-util
  (:use [clj-stripe http-util])
  (:require [clojure.test :as test]))

(test/deftest test-url-creation
  (let [stubs [["/customers" "customers"] ["/subscriptions" "subscriptions"] ["/tokens" "tokens"] ["/discount" "subscriptions-discount"]]]
    (test/is (= (name-to-kw "customers")
               :customer-id)
      "name-to-kw")
    (test/is (= (curate-stubs stubs)
               [["/customers" :customer-id] ["/subscriptions" :subscription-id] ["/tokens" :token-id] ["/discount"]])
      "curate-stubs")
    (test/is (= (build-url {:customer-id "ccc1" :subscription-id "sss1" :discount-amount 50} stubs)
               ["/customers/ccc1/subscriptions/sss1/tokens/discount" {:discount-amount 50}])
      "build-url")
    (test/is (= (kws-to-url-params {:test1 "test1" :test-2 {:test-3 3}})
               {"test1" "test1" "test_2" {"test_3" 3}})
      "kws-to-url-params")
    (test/is (= (remove-nils {:nil-test nil :reg-test 1})
               {:reg-test 1})
      "remove-nils")))

(test/deftest test-set-tokens!
  (test/is (= (set-tokens! {:private "private" :public "public"}) {:private "private" :public "public"}) "set-tokens! private and public")
  (test/is (= (set-tokens! {:private "privater"}) {:private "privater" :public "public"}) "set-tokens! private only")
  (test/is (= (set-tokens! {:public "publicer"}) {:private "privater" :public "publicer"}) "set-tokens! public only")
  (test/is (= (set-tokens! {}) {:private "privater" :public "publicer"}) "set-tokens! empty"))
