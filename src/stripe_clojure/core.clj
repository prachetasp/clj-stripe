;   Copyright (c) 2014 Alberto Bengoa/Prachetas Prabhu. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns stripe-clojure.core
  "Functions for Stripe Customers API"
  (:require [stripe-clojure.http-util :refer [do-request set-tokens!]]
            [clj-http.client :only [get post delete] :as client]))

(def url-vals {"cards" :card_id
               ;;"charges" :charge_id
               ;;"coupons" :coupon_id
               "customers" :customer_id
               "events" :event_id
               ;;"invoiceitems" :invoiceitem_id
               "invoices" :invoice_id
               "plans" :plan_id
               "subscriptions" :subscription_id
               "tokens" :token_id})

(defn build-url-map-vals [resources]
  [resources (map url-vals resources)])

;; resources lacking test coverage are commented out
;; contains a map like {:cards [["customers" "cards"] [:customer_id :card_id]]...}
(def url-mapping (into {}
                   (map (fn [[k v]] [k (build-url-map-vals v)])
                     {:cards ["customers" "cards"]
                      ;;:charges ["charges"]
                      ;;:charges-capture ["charges" "capture"] ; no-id endpt
                      ;;:charges-refund ["charges" "refund"] ; no-id endpt
                      ;;:coupons ["coupons"]
                      :customers ["customers"]
                      ;;:discounts ["customers" "discount"] ; no-id endpt
                      :events ["events"]
                      ;;:invoiceitems ["invoiceitems"]
                      :invoices ["invoices"]
                      ;;:invoices-lines ["invoices" "lines"] ; no-id endpt
                      ;;:invoices-pay ["invoices" "pay"] ; no id endpt
                      ;;:invoices-upcoming ["invoices" "upcoming"] ; no-id endpt
                      :plans ["plans"]
                      :subscriptions ["customers" "subscriptions"]
                      :tokens ["tokens"]})))

;; TODO: add params validation e.g. to make sure a get-list doesn't
;; contain an id
(defmacro defop [op-name http-action & {:keys [op]}]
  `(def ~op-name
     (fn [resource# params#]
       (do-request params# ~http-action
         (url-mapping (if ~op
                        (-> resource# name (str "-" ~op) keyword)
                        resource#))))))

;; operations lacking test coverage are commented out
(defop stripe-cancel client/delete)
#_(defop stripe-capture client/post :op "capture")
(defop stripe-create client/post)
(defop stripe-delete client/delete)
(defop stripe-get client/get)
(defop stripe-list client/get)
#_(defop stripe-pay client/post :op "pay")
#_(defop stripe-refund client/post :op "refund")
(defop stripe-update client/post)
