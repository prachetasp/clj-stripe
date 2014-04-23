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

(defn op-entity [operator operation & params]
  (operator {:op operation :params (apply merge params)}))

(defmacro test-crud-entity [operator params entity-kw]
  `(let [base-params# (or (:base ~params) {})
         {entity-id# :id :as create-entity-result#} (op-entity ~operator :create base-params# (:create ~params))
         entity-map# {~entity-kw entity-id#}
         ops-to-test# (:ops-to-test ~params)]
     (test/is (not (nil? entity-id#)))
     (test/is (= (op-entity ~operator :get entity-map# base-params#) create-entity-result#))
     (if (contains? ~params :update)
       (test/is (= (op-entity ~operator :update entity-map# base-params# (or (:update ~params) {})) (op-entity ~operator :get entity-map# base-params#))))
     (when (:test-delete? ~params)
       (op-entity ~operator :delete entity-map# base-params#)
       (test/is (nil? (some #{entity-id#} (map :id (:data (~operator {:op :get :params base-params#})))))))))

(test/deftest customers-test
  (test-crud-entity on-customers
    {:create test-customer
     :update {:email new-email}
     :test-delete? true}
    :customer-id))

(test/deftest card-tokens-test
  (let [{token-id :id :as create-token-result} (on-tokens {:op :create :params {:card test-card}})]
    (test/is (= (on-tokens {:op :get :params {:token-id token-id}}) create-token-result))))

(test/deftest subscriptions-test
  (test-crud-entity on-subscriptions
    {:create {:plan test-plan}
     :base {:customer-id existing-cust-id}
     :update {:quantity 2}
     :test-delete? true}
    :subscription-id))

(test/deftest events-test
  (let [{event-id :id :as recent-event} (first (:data (on-events {:op :get :params {}})))]
    (test/is (not (nil? event-id)))
    (test/is (= (on-events {:op :get :params {:event-id event-id}}) recent-event))))
