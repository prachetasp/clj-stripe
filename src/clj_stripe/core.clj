;   Copyright (c) 2014 Alberto Bengoa/Prachetas Prabhu. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clj-stripe.core
    "Functions for Stripe Customers API"
    (:require [clojure.walk :refer [prewalk]]
              [clj-http.client :only [get post delete] :as client]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UTILITY FUNCTIONS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; currently only supports private token
;; TODO: add support for token selection
(defonce stripe-tokens (atom {:public "" :private ""}))
(defn set-tokens! [m] (swap! stripe-tokens (fn [a] (merge a m))))

(defmacro with-token [token & body]
  `(binding [*stripe-token* ~token] ~@body))

;; Root URL for the API calls
(defonce api-root "https://api.stripe.com/v1")

;; Currently only does top-level
(defn- remove-nulls [m]
  (into {} (remove (comp nil? second) m)))

;; (params kw) so that it returns nil if key not found usually occurs if endpt doesn't
;; use an id and the entry in xxxx/url-mapping only has 1 element (url
;; stub) or if the user provides invalid data
(defn- build-url
  "Accepts url-stubs in the form of e.g. [\"/customers\" :customer]"
  [params url-stubs]
  (reduce (fn [[url d] [stub kw]]
            [(str url (str stub) (if-let [param (params kw)] (str "/" param) ""))
             (dissoc d kw)])
    ["" params]
    url-stubs))

(defn- kws-to-url-params [params]
  (prewalk #(if (keyword? %) (.replace (name %) "-" "_") %) params))

(defn- build-options [token params]
  {:basic-auth [token] :query-params (remove-nulls (kws-to-url-params params)) :throw-exceptions false :as :json :coerce :always})

(defn do-request
  [og-params method & url-stubs]
  (let [[url params] (build-url og-params url-stubs)]
    (try
      (:body (method (str api-root url) (build-options (:private @stripe-tokens) params)))
      (catch java.lang.Exception e e))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API FUNCTIONS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def url-mapping {:cards ["/cards" :card-id]
                  :charges ["/charges" :charge-id]
                  :charges-capture ["/capture"] ; no-id endpt
                  :charges-refund ["/refund"] ; no-id endpt
                  :coupons ["/coupons" :coupon-id]
                  :customers ["/customers" :customer-id]
                  ;;:discounts ["/discount"] ; no-id endpt
                  :events ["/events" :event-id]
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
                 :events {:get [client/get]}
                 :invoiceitems crud-mapping
                 :invoices {:create [client/post]
                            :get [client/get]
                            :get-lines [client/get [(:invoices-lines url-mapping)]]
                            :get-upcoming [client/get [(:invoices-upcoming url-mapping)]]
                            :pay [client/post [(:invoices-pay url-mapping)]]
                            :update [client/post]}
                 :plans crud-mapping
                 :subscriptions {:create [client/post [(:subscriptions url-mapping)] (:customers url-mapping)]
                                 :delete [client/delete [(:subscriptions url-mapping)] (:customers url-mapping)]
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
(defn on-events [op-params] (do-op op-params :events))
(defn on-invoiceitems [op-params] (do-op op-params :invoiceitems))
(defn on-invoices [op-params] (do-op op-params :invoices))
(defn on-plans [op-params] (do-op op-params :plans))
(defn on-subscriptions [op-params] (do-op op-params :subscriptions))
(defn on-tokens [op-params] (do-op op-params :tokens))
