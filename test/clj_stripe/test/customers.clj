;   Copyright (c) 2011 Alberto Bengoa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clj-stripe.test.core
  (:use [clj-stripe core util])
  (:require [clojure.test :as test]))

(with-token "YOUR SECRET TEST TOKEN HERE:"

  (def test-customer {:card {:number "4242424242424242" :exp-month 12 :exp-year 20 :cvc 123 :name "Mr. Stripe Clojure"}
                  :email "mrclojure@stripetest.com"
                      :description "customer test from clj-stripe"})

  (def create-customer-result (on-customers {:op :create :params test-customer}))
  (def customer-id (:id create-customer-result))
  (def get-customer-result (on-customers {:op :get :params {:customer-id customer-id}}))
  (def get-all-customers-result (on-customers {:op :get :params {}}))

  (test/deftest customers-test
    (test/is (= get-customer-result create-customer-result))
    (test/is (some #{customer-id} (map :id (:data get-all-customers-result)))))

  (def new-email "newmrclojure@stripetest.com")
  (def update-customer-result (on-customers {:op :update :params {:customer-id customer-id :email new-email}}))
  (def get-update-customer-result (on-customers {:op :get :params {:customer-id customer-id}}))

  (test/deftest modify-customer-test
    (test/is (= (assoc create-customer-result :email new-email) get-update-customer-result update-customer-result)))

  (def delete-customer-result (on-customers {:op :delete :params {:customer-id customer-id}}))
  (def get-all-customers-result-2 (on-customers {:op :get :params {}}))

  ;; This test works as long as you don't have too many customers (this
  ;; is not paginated)
  ;; TODO: add pagination
  (test/deftest delete-customer-test
    (test/is (nil? (some #{customer-id} (map :id (:data get-all-customers-result-2)))))))
