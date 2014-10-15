;   Copyright (c) 2014 Alberto Bengoa/Prachetas Prabhu. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns stripe-clojure.test.core
  (:use [stripe-clojure core])
  (:require [stripe-clojure.http-util :refer [set-tokens! name-to-kw]])
  (:require [stripe-clojure.test.core-config :refer [existing-cust-id secret-tokens test-plan]])
  (:require [clojure.test :refer [deftest is testing]]))

#_(deftest test-build-path
  (is (= (build-path :subscriptions) [["/customers" "customers"] ["/subscriptions" "subscriptions"]]) "Building resource path"))

(def crud-params #{:get :create :update :delete :base})
(def test-card {:number "4242424242424242" :exp_month 12 :exp_year 2020 :cvc 123 :name "Mr. Stripe Clojure"})
(def test-customer {:card test-card
                    :email "mrclojure@stripetest.com"
                    :description "customer test from clj-stripe"})

(set-tokens! secret-tokens)

;; Only tests CRUD for now
;; TODO: Add ability to specify assertion for non-crud operations
(defmacro deftest-resource [r-kw params]
  `(let [bps# (merge {} (:base ~params))
         ;;do-op# (fn [op# r# & ps#] (op# r# (apply merge ps# (:base ~params))))
         {r-id# :id :as r-result#} (if-let [ps# (:create ~params)]
                                     (stripe-create ~r-kw (merge ps# bps#))
                                     (stripe-get ~r-kw (merge (:get ~params) bps#)))
         r-map# {(name-to-kw (name ~r-kw)) r-id#}
         ;;non-crud-ops# (select-keys ~params (filter #(not (contains? crud-params %)) (keys ~params)))
         ]
     (testing "create (or first get for non-create resources)" (is (not (nil? r-id#))))
     (testing "get") (is (= (stripe-get ~r-kw (merge r-map# bps#)) r-result#))
     (when-let [ps# (:update ~params)]
       (testing "update" (is (= (stripe-update ~r-kw (merge r-map# ps# bps#)) (stripe-get ~r-kw (merge r-map# bps#))))))
     #_(doseq [[op# op-params#] non-crud-ops#]
         (op# ~r-kw (merge r-map# op-params# bps#)))
     (if (:delete ~params)
       ((:delete ~params) ~r-kw (merge r-map# bps#))
       (testing "delete/cancel" (is (every? false? (map #(= r-id# (:id %)) (:data (stripe-list ~r-kw bps#)))))))))

(deftest cards-test
  (testing "Cards"
      (deftest-resource :cards
        {:base {:customer_id existing-cust-id}
         :create {:card test-card}
         :update {:name "Mr. NewStripe Clojure"}
         :delete stripe-delete})))

(deftest card-tokens-test
  (testing "Card Tokens"
    (let [{token-id :id :as create-token-result} (stripe-create :tokens {:card test-card})]
      (testing "create" (is (not (nil? token-id))))
      (testing "get" (is (= (stripe-get :tokens {:token_id token-id}) create-token-result))))))

(deftest customers-test
  (testing "Customers"
    (deftest-resource :customers
      {:create test-customer
       :update {:email "newmrclojure@stripetest.com"}
       :delete stripe-cancel})))

(deftest events-test
  (testing "Events"
    (let [{event-id :id :as recent-event} (-> (stripe-get :events {}) :data first)]
      (testing "list" (is (not (nil? event-id))))
      (testing "get" (is (= (stripe-get :events {:event_id event-id}) recent-event))))))

(deftest subscriptions-test
  (testing "Subscriptions"
    (deftest-resource :subscriptions
      {:create {:plan test-plan}
       :base {:customer_id existing-cust-id}
       :update {:quantity 2}
       :delete stripe-delete})))
