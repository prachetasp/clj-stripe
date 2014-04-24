;   Copyright (c) 2014 Alberto Bengoa/Prachetas Prabhu. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UNIT TESTS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#_(ns clj-stripe.core
  (:require [clojure.test :as test]))

#_(test/deftest test-url-creation
  (let [stubs [["/customers" "customers"] ["/subscriptions" "subscriptions"] ["/tokens" "tokens"] ["/discount" "subscriptions-discount"]]]
    (test/is (= (name-to-kw "customers")
               :customer-id))
    (test/is (= (curate-stubs stubs)
               [["/customers" :customer-id] ["/subscriptions" :subscription-id] ["/tokens" :token-id] ["/discount"]]))
    (test/is (= (build-url {:customer-id "ccc1" :subscription-id "sss1" :discount-amount 50} stubs)
               ["/customers/ccc1/subscriptions/sss1/tokens/discount" {:discount-amount 50}]))
    (test/is (= (kws-to-url-params {:test1 "test1" :test-2 {:test-3 3}})
               {"test1" "test1" "test_2" {"test_3" 3}}))
    (test/is (= (remove-nils {:nil-test nil :reg-test 1})
               {:reg-test 1}))))

#_(test/deftest test-set-tokens!
  (test/is (= (set-tokens! {:private "private" :public "public"}) {:private "private" :public "public"}))
  (test/is (= (set-tokens! {:private "private"}) {:private "private"}))
  (test/is (= (set-tokens! {:public "public"})) {:public "public"})
  (test/is (= (set-tokens! {}) {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; FUNCTIONAL TESTS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(ns clj-stripe.test.core
  (:use [clj-stripe core])
  (:require [clj-stripe.test.core-config :refer [existing-cust-id secret-tokens test-plan]])
  (:require [clojure.test :as test]))

(def crud-params #{:create :update :test-delete? :base})
(def test-card {:number "4242424242424242" :exp-month 12 :exp-year 2020 :cvc 123 :name "Mr. Stripe Clojure"})
(def test-customer {:card test-card
                    :email "mrclojure@stripetest.com"
                    :description "customer test from clj-stripe"})
(def new-email "newmrclojure@stripetest.com")
(defn op-entity [operator operation & params]
  (operator {:op operation :params (apply merge params)}))

(set-tokens! secret-tokens)

;; Only tests CRUD for now
;; TODO: Add ability to specify assertion for non-crud operations
#_(defmacro test-entity [operator params entity-kw]
  `(let [base-params# (or (:base ~params) {})
         {entity-id# :id :as create-entity-result#} (if (contains? ~params :create)
                                                      (op-entity ~operator :create base-params# (:create ~params))
                                                      (op-entity ~operator :get base-params# {~entity-kw (~entity-kw ~params)}))
         entity-map# {~entity-kw entity-id#}
         ops-to-test# (:ops-to-test ~params)
         non-crud-ops# (select-keys ~params (filter #(not (contains? crud-params %)) (keys ~params)))]
     (test/is (not (nil? entity-id#)))
     (test/is (= (op-entity ~operator :get entity-map# base-params#) create-entity-result#))
     (when (contains? ~params :update)
       (test/is (= (op-entity ~operator :update entity-map# base-params# (or (:update ~params) {})) (op-entity ~operator :get entity-map# base-params#))))
     #_(doseq [[op# op-params#] non-crud-ops#]
       (op-entity ~operator op# entity-map# base-params# op-params#))
     (when (:test-delete? ~params)
       (op-entity ~operator :delete entity-map# base-params#)
       (test/is (nil? (some #{entity-id#} (map :id (:data (~operator {:op :get :params base-params#})))))))))

#_(test/deftest customers-test
  (test-entity on-customers
    {:create test-customer
     :update {:email new-email}
     :test-delete? true}
    :customer-id))

(test/deftest card-tokens-test
  (let [{token-id :id :as create-token-result} (stripe-create :tokens {:card test-card})]
    (test/is (not (nil? token-id)))
    (test/is (= (stripe-get :tokens {:token-id token-id}) create-token-result))))

#_(test/deftest subscriptions-test
  (test-entity on-subscriptions
    {:create {:plan test-plan}
     :base {:customer-id existing-cust-id}
     :update {:quantity 2}
     :test-delete? true}
    :subscription-id))

(test/deftest events-test
  (let [{event-id :id :as recent-event} (-> (stripe-get :events {}) :data first)]
    (test/is (not (nil? event-id)))
    (test/is (= (stripe-get :events {:event-id event-id}) recent-event))))

(def new-customer (stripe-create :customers (merge test-customer {:plan test-plan})))
#_(test/deftest invoices-test
  (when (not (contains? new-customer :error))
    (Thread/sleep 500) ; just in case the invoice hasn't processed yet
    (let [invoice-resp (on-invoices {:op :get :params {:customer (:id new-customer)}})]
      (test-entity on-invoices
        {:invoice-id (get-in invoice-resp [:data 0 :id])
         :update {:description "clj-stripe invoice update"}}
        :invoice-id))))
(stripe-delete :customers {:customer-id (:id new-customer)})
