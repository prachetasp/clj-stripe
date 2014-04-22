;   Copyright (c) 2011 Alberto Bengoa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clj-stripe.core
    "Functions for Stripe Customers API"
    (:require [clj-stripe.util :refer [do-request with-token]]
              [clj-http.client :only [get post delete] :as client]))

(def url-mapping {:cards ["/cards" :card-id]
                  :charges ["/charges" :charge-id]
                  :charges-capture ["/capture"] ; no-id endpt
                  :charges-refund ["/refund"] ; no-id endpt
                  :coupons ["/coupons" :coupon-id]
                  :customers ["/customers" :customer-id]
                  ;;:discounts ["/discount"] ; no-id endpt
                  :invoiceitems ["/invoiceitems" :invoiceitem-id]
                  :invoices ["/invoices" :invoice-id]
                  :invoices-lines ["/lines"] ; no-id endpt
                  :invoices-pay ["/pay"] ; no id endpt
                  :invoices-upcoming ["/upcoming"] ; no-id endpt
                  :plans ["/plans" :plan-id]
                  :subscriptions ["/subscriptions" :subscription-id]
                  :tokens ["/tokens" :token-id]})

(defonce crud-mapping {:create [client/post]
                       :delete [client/delete]
                       :get [client/get]
                       :update [client/post]})

(def op-mapping {:charges {:capture [client/post [(:charges-capture url-mapping)]]
                           :create [client/post]
                           :get [client/get]
                           :refund [client/post [(:charges-refund url-mapping)]]}
                 :coupons {:create [client/post]
                           :delete [client/delete]
                           :get [client/get]}
                 :customers crud-mapping
                 :invoiceitems crud-mapping
                 :invoices {:create [client/post]
                            :get [client/get]
                            :get-lines [client/get [(:invoices-lines url-mapping)]]
                            :get-upcoming [client/get [(:invoices-upcoming url-mapping)]]
                            :pay [client/post [(:invoices-pay url-mapping)]]
                            :update [client/post]}
                 :plans crud-mapping
                 :subscriptions {:cancel [client/delete [(:subscriptions url-mapping)] (:customers url-mapping)]
                                 :create [client/post [(:subscriptions url-mapping)] (:customers url-mapping)]
                                 :get [client/get [(:subscriptions url-mapping)] (:customers url-mapping)]
                                 :update [client/post [(:subscriptions url-mapping)] (:customers url-mapping)]}
                 :tokens {:create [client/post]
                          :get [client/get]}})

;; properly formats method and url
(defn get-op-details [entity op]
  (let [[method extra-paths base-path] (get-in op-mapping [entity op])]
    (into [method] (into [(or base-path (entity url-mapping))] (or extra-paths [])))))

(defn do-op [op-params entity] (apply do-request (:params op-params) (get-op-details entity (:op op-params))))

(defn on-charges [op-params] (do-op op-params :charges))
(defn on-coupons [op-params] (do-op op-params :coupons))
(defn on-customers [op-params] (do-op op-params :customers))
(defn on-invoiceitems [op-params] (do-op op-params :invoiceitems))
(defn on-invoices [op-params] (do-op op-params :invoices))
(defn on-plans [op-params] (do-op op-params :plans))
(defn on-subscriptions [op-params] (do-op op-params :subscriptions))
(defn on-tokens [op-params] (do-op op-params :tokens))
