;   Copyright (c) 2014 Alberto Bengoa/Prachetas Prabhu. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clj-stripe.test.core
  (:use [clj-stripe core])
  (:require [clj-stripe.test.core-config :refer [secret-tokens existing-cust-id test-plan]])
  (:require [clojure.test :as test]))

(set-tokens! secret-tokens)

(def test-card {:number "4242424242424242" :exp-month 12 :exp-year 2020 :cvc 123 :name "Mr. Stripe Clojure"})
(def test-customer {:card test-card
                    :email "mrclojure@stripetest.com"
                    :description "customer test from clj-stripe"})
(def new-email "newmrclojure@stripetest.com")


(defn op-customer [op cust-id & {:keys [extra-params] :or {extra-params {}}}] (on-customers {:op op :params (merge {:customer-id cust-id} extra-params)}))
(defn get-all-customers [] (on-customers {:op :get :params {}}))
(defn test-customer-exists [cust-id] (some #{cust-id} (map :id (:data (get-all-customers)))))

(test/deftest customers-test
  (let [{cust-id :id :as create-cust-result} (on-customers {:op :create :params test-customer})]
    (test/is (not (nil? cust-id)))  ; if this fails the rest of the customer
                                        ; tests are probably screwed
    (test/is (= (op-customer :get cust-id) create-cust-result))
    (test/is (= (op-customer :update cust-id :extra-params {:email new-email}) (op-customer :get cust-id)))
    (op-customer :delete cust-id)
    ;; This test works as long as you don't have too many customers
    ;; TODO: add pagination
    (test/is (nil? (test-customer-exists cust-id)))))

(test/deftest card-tokens-test
  (let [{token-id :id :as create-token-result} (on-tokens {:op :create :params {:card test-card}})]
    (test/is (= (on-tokens {:op :get :params {:token-id token-id}}) create-token-result))))

(defn op-subscription [op sub-id cust-id & {:keys [extra-params] :or {extra-params {}}}]
  (on-subscriptions {:op op :params (merge {:customer-id cust-id :subscription-id sub-id} extra-params)}))
(defn get-all-subscriptions [cust-id] (on-subscriptions {:op :get :params {:customer-id cust-id}}))
(defn test-subscription-exists [sub-id cust-id] (some #{sub-id} (map :id (:data (get-all-subscriptions cust-id)))))

(test/deftest subscriptions-test
  (let [{sub-id :id :as create-sub-result} (on-subscriptions {:op :create :params {:customer-id existing-cust-id :plan test-plan}})]
    (test/is (not (nil? sub-id)))  ; if this fails the rest of the subscription
                                        ; tests are probably screwed
    (test/is (= (op-subscription :get sub-id existing-cust-id) create-sub-result))
    (test/is (= (op-subscription :update sub-id existing-cust-id :extra-params {:quantity 2}) (op-subscription :get sub-id existing-cust-id)))
    (op-subscription :cancel sub-id existing-cust-id)
    ;; This test works as long as you don't have too many customers
    ;; TODO: add pagination
    (test/is (nil? (test-subscription-exists sub-id existing-cust-id)))))

(test/deftest events-test
  (let [{event-id :id :as recent-event} (first (:data (on-events {:op :get :params {}})))]
    (test/is (not (nil? event-id)))
    (test/is (= (on-events {:op :get :params {:event-id event-id}}) recent-event))))
