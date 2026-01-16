(ns fixtures-test
  (:require [repl]
            [com.biffweb :as biff :refer [q]]))

(defn setup!
  "Fixture データを投入"
  []
  (repl/add-fixtures))


(defn all-users
  "全ユーザーを取得"
  []
  (let [{:keys [biff/db]} (repl/get-context)]
    (q db
       '{:find [(pull user [*])]
         :where [[user :user/email]]})))

(defn users-with-customer-id
  "Stripe Customer ID を持つユーザー"
  []
  (let [{:keys [biff/db]} (repl/get-context)]
    (q db
       '{:find [(pull user [*])]
         :where [[user :user/stripe-customer-id]]})))

(defn all-payments
  "全決済記録"
  []
  (let [{:keys [biff/db]} (repl/get-context)]
    (q db
       '{:find [(pull payment [*])]
         :where [[payment :payment/stripe-session-id]]})))

(defn guest-payments
  "ゲスト決済のみ"
  []
  (let [{:keys [biff/db]} (repl/get-context)]
    (q db
       '{:find [(pull payment [*])]
         :where [[payment :payment/user nil]]})))

(defn all-webhook-events
  "全 Webhook イベント"
  []
  (let [{:keys [biff/db]} (repl/get-context)]
    (q db
       '{:find [(pull event [*])]
         :where [[event :webhook-event/stripe-id]]})))



(comment
  (setup!)
  :rcf)

(comment
  ;; 2. 確認
  (all-users)
  (users-with-customer-id)
  (all-payments)
  (guest-payments)
  (all-webhook-events)

  :rcf)

(comment
  ;; 必須フィールドがないとエラー
  (biff/submit-tx (repl/get-context)
                  [{:db/doc-type :payment
                    :xt/id (random-uuid)
                    ;; :payment/stripe-session-id がない → エラー
                    :payment/amount 1000
                    :payment/currency "jpy"
                    :payment/status "succeeded"
                    :payment/created-at (java.util.Date.)}])

  ;; closed map なので未定義フィールドもエラー
  (biff/submit-tx (repl/get-context)
                  [{:db/doc-type :user
                    :xt/id (random-uuid)
                    :user/email "test@example.com"
                    :user/joined-at (java.util.Date.)
                    :user/unknown-field "xxx"}])  ;; → エラー

  :rcf)