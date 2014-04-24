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
(defn- remove-nils [m]
  (into {} (remove (comp nil? second) m)))

;; removes the s and adds _id e.g. customers -> customer_id
(defn- name-to-kw [r-name]
  (keyword (str (.substring r-name 0 (- (.length r-name) 1)) "-id")))

;; remove resource action names that have no id (pay, refund, capture
;; etc.) and convert remaining names to kw's that match the params
(defn- curate-stubs [url-stubs]
  (reduce (fn [v [stub r-name]]
            (if (.contains r-name "-")
              (conj v [stub])
              (conj v [stub (name-to-kw r-name)])))
    []
    url-stubs))

;; (params kw) so that it returns nil if key not found usually occurs if
;; endpt doesn't use an id and the entry in xxxx/url-mapping only has 1
;; element (url stub) or if the user provides invalid data
(defn- build-url
  "Accepts url-stubs in the form of e.g. [\"/pay\" \"invoices-pay\"]"
  [params url-stubs]
  (reduce (fn [[url prms] [stub kw]]
            [(str url stub (if kw (if-let [p (params kw)] (str "/" p) "") ""))
             (if kw (dissoc prms kw) prms)])
    ["" params]
    (curate-stubs url-stubs)))


(defn- kws-to-url-params [params]
  (prewalk #(if (keyword? %) (.replace (name %) "-" "_") %) params))

(defn- build-options [token params]
  {:basic-auth [token] :query-params (remove-nils (kws-to-url-params params)) :throw-exceptions false :as :json :coerce :always})

(defn do-request
  [og-params method url-stubs]
  (let [[url params] (build-url og-params url-stubs)]
    (try
      (:body (method (str api-root url) (build-options (:private @stripe-tokens) params)))
      (catch java.lang.Exception e e))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; API FUNCTIONS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def url-mapping {:cards {:url "/cards"}
                  :charges {:url "/charges"}
                  :charges-capture {:url "/capture" :base :charges} ; no-id endpt

                  :charges-refund {:url "/refund" :base :charges} ; no-id endpt
                  :coupons {:url "/coupons"}
                  :customers {:url "/customers"}
                  ;;:discounts ["/discount"] ; no-id endpt
                  :events {:url "/events"}
                  :invoiceitems {:url "/invoiceitems"}
                  :invoices {:url "/invoices"}
                  :invoices-lines {:url "/lines" :base :invoices} ; no-id endpt
                  :invoices-pay {:url "/pay" :base :invoices} ; no id endpt
                  :invoices-upcoming {:url "/upcoming" :base :invoices} ; no-id endpt
                  :plans {:url "/plans"}
                  :subscriptions {:url "/subscriptions" :base :customers}
                  :tokens {:url "/tokens"}})

;; assembles url stubs in path order
(defn build-path [resource]
  (let [get-base #(-> url-mapping % :base)]
    (loop [res resource path []]
      (if (nil? res)
        path
        (recur (get-base res) (conj path [(-> url-mapping res :url) (name res)]))))))

;; TODO: add params validation e.g. to make sure a get-list doesn't
;; contain an id
(defmacro defop [op-name http-action & {:keys [op]}]
  `(def ~op-name
     (fn [resource# params#]
       (do-request params# ~http-action (build-path (if ~op
                                                      (-> resource# name (str "-" ~op) keyword)
                                                      resource#))))))

(defop stripe-cancel client/delete)
(defop stripe-capture client/post :op "capture")
(defop stripe-create client/post)
(defop stripe-delete client/delete)
(defop stripe-get client/get)
(defop stripe-list client/get)
(defop stripe-pay client/post :op "pay")
(defop stripe-refund client/post :op "refund")
(defop stripe-update client/post)
