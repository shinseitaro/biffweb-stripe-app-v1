(ns com.myapp.billing.webhooks
  (:require [com.biffweb :as biff]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [xtdb.api :as xt])
  (:import [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))


;; HMAC-SHA256 署名を計算
(defn- hmac-sha256 [secret message]
  (let [mac (Mac/getInstance "HmacSHA256")
        key (SecretKeySpec. (.getBytes secret "UTF-8") "HmacSHA256")]
    (.init mac key)
    (-> (.doFinal mac (.getBytes message "UTF-8"))
        (->> (map #(format "%02x" %))
             (apply str)))))

;; Stripe 署名の検証

(defn- verify-signiture [{:keys [payload signature secret]}]
  (let [[_ timestamp] (re-find #"t=(\d+)" signature)
        [_ v1-sig] (re-find #"v1=([a-f0-9]+)" signature)
        signed-payload (str timestamp "." payload)
        expected-sig (hmac-sha256 secret signed-payload)]
    (= v1-sig expected-sig)))

(defn-  find-user-id-by-customer-id [db customer-id]
  (when customer-id
    (biff/lookup-id db :user/stripe-customer-id customer-id)))

(defn- save-payment! [{:keys [biff/db] :as ctx}
                      {:keys [customer id amount currency status]}]
  (let [user-id (find-user-id-by-customer-id db customer)
        payment-id (random-uuid)
        payment-doc {:db/doc-type :payment
                     :xt/id payment-id
                     :payment/stripe-payment-intent-id id
                     :payment/amount amount
                     :payment/currency currency
                     :payment/status status
                     :payment/created-at (biff/now)
                     :payment/user user-id}]
    (biff/submit-tx ctx [payment-doc])
    (log/info "Payment saved:" payment-id
              "user:" user-id
              "amount:" amount)))


;; イベント処理
(defn- handle-event [ctx event]
  (let [event-type (:type event)
        data (get-in event [:data :object])]

    (log/info "Webhook received:" event-type)

    (case event-type
      "checkout.session.completed"
      (log/info "Checkout completed:" (:id data))

      "payment_intent.succeeded"
      (do
        (log/info "Payment completed:" (:id data))
        (save-payment! ctx data))

      "payment_intent.payment_failed"
      (log/warn "Payment failed:" (:id data) "reason:" (get-in data [:last_payment_error :message]))

      (log/info "Unhandled event type:" event-type))))

;; webhook エンドポイント
(defn webhook [{:keys [biff/secret headers body] :as ctx}]
  (let [payload (slurp body)
        signature (get headers "stripe-signature")
        webhook-secret (secret :stripe/webhook-secret)]
    (if (verify-signiture {:payload payload
                           :signature signature
                           :secret webhook-secret})
      (do
        (handle-event ctx (json/parse-string payload true))
        {:status 200
         :body "OK"})

      (do
        (log/warn "Invalid webhook signature")
        {:status 400
         :body "Invalid signature"}))))

(def module
  {:raw-api-routes [["/api/webhook" {:post webhook}]]})

