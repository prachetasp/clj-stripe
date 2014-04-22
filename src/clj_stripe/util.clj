;   Copyright (c) 2011 Alberto Bengoa. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clj-stripe.util
  (:require [clj-http.client :as client])
  (:require [clojure.walk :refer [prewalk]]))

(defonce ^:dynamic *stripe-token* nil)

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
  {:basic-auth [token] :query-params (remove-nulls (kws-to-url-params params)) :throw-exceptions false :as :json})

(defn make-request
  [params method url]
  (try
     (:body (method (str api-root url) (build-options *stripe-token* params)))
     (catch java.lang.Exception e e)))

(defn do-request [og-params method & url-stubs]
  (let [[url params] (build-url og-params url-stubs)]
    (make-request params method url)))
