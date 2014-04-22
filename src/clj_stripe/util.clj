;   Copyright (c) 2011 Alberto Bengoa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clj-stripe.util
	(:require [clj-http.client :as client]))

(defonce ^:dynamic *stripe-token* nil)

(defmacro with-token
  "Binds the specified Stripe authentication token to the stripe-token variable and executes the body."
  [token & body]
  `(binding [*stripe-token* ~token] ~@body))

;; Root URL for the API calls
(defonce api-root "https://api.stripe.com/v1")

(defn- remove-nulls
  "Removes from a map the keys with nil value."
  [m]
  (into {} (remove (comp nil? second) m)))

(defn build-url
  "Accepts url-stubs in the form of e.g. [\"/customers\" :customer]"
  [params url-stubs]
  (reduce (fn [[url d] [stub kw]]
            [(str url (str stub) (if-let [param (kw params)] (str "/" param) ""))
             (dissoc d kw)])
    ["" params]
    url-stubs))

(defn kws-to-url-params [params]
  (into {} (for [[k v] params] [(.replace (name k) "-" "_") v])))

(defn build-options [token params]
  {:basic-auth [token] :query-params (remove-nulls (kws-to-url-params params)) :throw-exceptions false :as :json})

;; Doesn't really need to be a macro but saves reflection on the method
;; Use if performance dictates?
#_(defmacro make-request
  "POSTs a to a url using the provided authentication token and parameters."
  [method token url params]
  `(try
     (:body (~method (str ~api-root ~url) (build-options ~token ~params)))
     (catch java.lang.Exception e# e#)))

(defn make-request
  "POSTs a to a url using the provided authentication token and parameters."
  [method token url params]
  (try
     (:body (method (str api-root url) (build-options token params)))
     (catch java.lang.Exception e e)))

(defn do-request [method token og-params & url-stubs]
  (let [[url params] (build-url og-params url-stubs)]
    (make-request method token url params)))
