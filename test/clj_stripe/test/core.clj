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

(def crud-params #{:create :update :test-delete? :base})
(def test-card {:number "4242424242424242" :exp-month 12 :exp-year 2020 :cvc 123 :name "Mr. Stripe Clojure"})
(def test-customer {:card test-card
                    :email "mrclojure@stripetest.com"
                    :description "customer test from clj-stripe"})
(def new-email "newmrclojure@stripetest.com")
(defn op-entity [operator operation & params]
  (operator {:op operation :params (apply merge params)}))

;; Only tests CRUD for now
;; TODO: Add ability to specify assertion for non-crud operations
(defmacro test-entity [operator params entity-kw]
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

(test/deftest customers-test
  (test-entity on-customers
    {:create test-customer
     :update {:email new-email}
     :test-delete? true}
    :customer-id))

(test/deftest card-tokens-test
  (let [{token-id :id :as create-token-result} (on-tokens {:op :create :params {:card test-card}})]
    (test/is (= (on-tokens {:op :get :params {:token-id token-id}}) create-token-result))))

(test/deftest subscriptions-test
  (test-entity on-subscriptions
    {:create {:plan test-plan}
     :base {:customer-id existing-cust-id}
     :update {:quantity 2}
     :test-delete? true}
    :subscription-id))

(test/deftest events-test
  (let [{event-id :id :as recent-event} (-> (on-events {:op :get :params {}}) :data first)]
    (test/is (not (nil? event-id)))
    (test/is (= (on-events {:op :get :params {:event-id event-id}}) recent-event))))

(def new-customer (op-entity on-customers :create (merge test-customer {:plan test-plan})))
(test/deftest invoices-test
  (when (not (contains? new-customer :error))
    (Thread/sleep 500) ; just in case the invoice hasn't processed yet
    (let [invoice-resp (on-invoices {:op :get :params {:customer (:id new-customer)}})]
      (test-entity on-invoices
        {:invoice-id (get-in invoice-resp [:data 0 :id])
         :update {:description "clj-stripe invoice update"}}
        :invoice-id))))
(on-customers {:op :delete :params {:customer-id (:id new-customer)}})
