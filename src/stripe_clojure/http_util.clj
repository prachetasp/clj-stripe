;   Copyright (c) 2014 Alberto Bengoa/Prachetas Prabhu. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns stripe-clojure.http-util
    "HTTP Utility Functions for Stripe Customers API")

;; currently only supports private token
;; TODO: add support for token selection
(defonce stripe-tokens (atom {:public "" :private ""}))
(defn set-tokens! [m] (swap! stripe-tokens (fn [a] (merge a m))))

;; Root URL for the API calls
(defonce api-root "https://api.stripe.com/v1/")

;; Currently only does top-level
(defn remove-nils [m]
  (into {} (remove (comp nil? second) m)))

;; removes the s and adds _id e.g. customers -> customer_id
(defn name-to-kw [r-name]
(keyword (str (.substring r-name 0 (- (.length r-name) 1)) "_id")))

(defn build-url [[url-ks url-vs] params]
  (str api-root
    (apply str
      (interpose "/"
        (filter (comp not nil?)
          (interleave url-ks (map params url-vs)))))))

(defn do-request
  [params method resource]
  (try
    (:body (method (build-url resource params)
             {:basic-auth [(:private @stripe-tokens)]
              :query-params (apply dissoc params (second resource))
              :throw-exceptions false
              :as :json
              :coerce :always}))
    (catch java.lang.Exception e e)))
