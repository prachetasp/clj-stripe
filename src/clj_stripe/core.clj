;   Copyright (c) 2014 Alberto Bengoa/Prachetas Prabhu. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clj-stripe.core
  "Functions for Stripe Customers API"
  (:require [clj-stripe.http-util :refer [do-request set-tokens!]]
            [clj-http.client :only [get post delete] :as client]))

;; resources lacking test coverage are commented out
(def url-mapping {:cards {:url "/cards" :base :customers}
                  ;;:charges {:url "/charges"}
                  ;;:charges-capture {:url "/capture" :base :charges} ; no-id endpt
                  ;;:charges-refund {:url "/refund" :base :charges} ; no-id endpt
                  ;;:coupons {:url "/coupons"}
                  :customers {:url "/customers"}
                  ;;:discounts ["/discount"] ; no-id endpt
                  :events {:url "/events"}
                  ;;:invoiceitems {:url "/invoiceitems"}
                  :invoices {:url "/invoices"}
                  ;;:invoices-lines {:url "/lines" :base :invoices} ; no-id endpt
                  ;;:invoices-pay {:url "/pay" :base :invoices} ; no id endpt
                  ;;:invoices-upcoming {:url "/upcoming" :base :invoices} ; no-id endpt
                  ;;:plans {:url "/plans"}
                  :subscriptions {:url "/subscriptions" :base :customers}
                  :tokens {:url "/tokens"}})

;; assembles url stubs in path order
;; uses list internally to conj at front
(defn build-path [resource]
  (into []
    (let [get-base #(-> url-mapping % :base)]
      (loop [res resource path '()]
        (if (nil? res)
          path
          (recur (get-base res) (conj path [(-> url-mapping res :url) (name res)])))))))

;; TODO: add params validation e.g. to make sure a get-list doesn't
;; contain an id
(defmacro defop [op-name http-action & {:keys [op]}]
  `(def ~op-name
     (fn [resource# params#]
       (do-request params# ~http-action (build-path (if ~op
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
