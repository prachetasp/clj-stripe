;   Copyright (c) 2014 Alberto Bengoa/Prachetas Prabhu. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clj-stripe.test.core
  (:use [clj-stripe core])
  (:require [clj-stripe.http-util :refer [set-tokens! name-to-kw]])
  (:require [clj-stripe.test.core-config :refer [existing-cust-id secret-tokens test-plan]])
  (:require [clojure.test :refer [deftest is testing]]))

(deftest test-build-path
  (is (= (build-path :subscriptions) [["/customers" "customers"] ["/subscriptions" "subscriptions"]]) "Building resource path"))

(def crud-params #{:get :create :update :delete :base})
(def test-card {:number "4242424242424242" :exp-month 12 :exp-year 2020 :cvc 123 :name "Mr. Stripe Clojure"})
(def test-customer {:card test-card
                    :email "mrclojure@stripetest.com"
                    :description "customer test from clj-stripe"})

(set-tokens! secret-tokens)

;; Only tests CRUD for now
;; TODO: Add ability to specify assertion for non-crud operations
(defmacro test-resource [r-kw params]
  `(let [base-params# (or (:base ~params) {})
         do-op# (fn [op# r# & ps#] (op# r# (apply merge base-params# ps#)))
         {r-id# :id :as r-result#} (if-let [ps# (:create ~params)]
                                     (do-op# stripe-create ~r-kw ps#)
                                     (do-op# stripe-get ~r-kw (:get ~params)))
         r-map# {(name-to-kw (name ~r-kw)) r-id#}
         ;;non-crud-ops# (select-keys ~params (filter #(not (contains? crud-params %)) (keys ~params)))
         ]
     (testing "create (or first get for non-create resources)" (is (not (nil? r-id#))))
     (testing "get") (is (= (do-op# stripe-get ~r-kw r-map#) r-result#))
     (when-let [ps# (:update ~params)]
       (testing "update" (is (= (do-op# stripe-update ~r-kw r-map# ps#) (do-op# stripe-get ~r-kw r-map#)))))
     #_(doseq [[op# op-params#] non-crud-ops#]
         (do-op# op# ~r-kw r-map# op-params#))
     (when-let [d-op# (:delete ~params)]
       (do-op# d-op# ~r-kw r-map#)
       (testing "delete/cancel" (is (nil? (some #{r-id#} (map :id (:data (do-op# stripe-list ~r-kw))))))))))

(deftest cards-test
  (testing "Cards"
      (test-resource :cards
        {:base {:customer-id existing-cust-id}
         :create {:card test-card}
         :update {:name "Mr. NewStripe Clojure"}
         :delete stripe-delete})))

(deftest card-tokens-test
  (testing "Card Tokens"
    (let [{token-id :id :as create-token-result} (stripe-create :tokens {:card test-card})]
      (testing "create" (is (not (nil? token-id))))
      (testing "get" (is (= (stripe-get :tokens {:token-id token-id}) create-token-result))))))

(deftest customers-test
  (testing "Customers"
    (test-resource :customers
      {:create test-customer
       :update {:email "newmrclojure@stripetest.com"}
       :delete stripe-cancel})))

(deftest events-test
  (testing "Events"
    (let [{event-id :id :as recent-event} (-> (stripe-get :events {}) :data first)]
      (testing "list" (is (not (nil? event-id))))
      (testing "get" (is (= (stripe-get :events {:event-id event-id}) recent-event))))))

(deftest subscriptions-test
  (testing "Subscriptions"
    (test-resource :subscriptions
      {:create {:plan test-plan}
       :base {:customer-id existing-cust-id}
       :update {:quantity 2}
       :delete stripe-delete})))
