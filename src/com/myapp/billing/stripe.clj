(ns com.myapp.billing.stripe
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [com.biffweb :as biff]
            [xtdb.api :as xt]
            [clojure.tools.logging :as log]
            [ring.util.response :as response]))

(defn- stripe-request
  "Stripe API へのリクエストを実行"
  [{:keys [method endpoint params secret-key]}]
  (let [url (str "https://api.stripe.com/v1" endpoint)
        response (http/request
                  {:method method
                   :url url
                   :basic-auth [secret-key ""]
                   :form-params params
                   :throw-exceptions false})
        body (-> response :body (json/parse-string true))]
    (if-let [error (:error body)]
      (do
        (log/error "Stripe API error:" (:type error) "-" (:message error)
                   "endpoint:" endpoint)
        nil)
      body)))

(defn create-customer
  "Stripe Customer を作成"
  [{:keys [biff/secret]} email]
  (let [secret-key (secret :stripe/secret-key)]
    (stripe-request {:method :post
                     :endpoint "/customers"
                     :params {:email email}
                     :secret-key secret-key})))

(defn get-customer
  "Stripe Customer を取得"
  [{:keys [biff/secret]} customer-id]
  (let [secret-key (secret :stripe/secret-key)]
    (stripe-request {:method :get
                     :endpoint (str "/customers/" customer-id)
                     :params {}
                     :secret-key secret-key})))

(defn find-customer-by-email
  "メールアドレスで既存の Customer を検索"
  [{:keys [biff/secret]} email]
  (let [secret-key (secret :stripe/secret-key)
        result (stripe-request
                {:method :get
                 :endpoint "/customers"
                 :params {:email email :limit 1}
                 :secret-key secret-key})]
    (first (:data result))))


(comment
  (require '[repl])
  (def ctx (repl/get-context))

  (create-customer ctx "alice@example.com")
  (get-customer ctx "cus_TnkMrV6SSbq7mC")

  (find-customer-by-email ctx "alice@example.com")
  (get-customer ctx "cus_invalid_id")
  :rcf)

(defn get-or-create-customer
  "ユーザーの Stripe Customer を取得。なければ作成して XTDB に保存"
  [{:keys [biff/db] :as ctx} user-id]
  (let [user (xt/entity db user-id)
        email (:user/email user)]
    (cond
      ;; ユーザーが存在しない場合
      (nil? user)
      (do
        (log/error "User not found:" user-id)
        nil)

      ;; メールアドレスがない場合
      (nil? email)
      (do
        (log/error "User has no email:" user-id)
        nil)

      ;; 既に Customer ID がある場合は取得
      (:user/stripe-customer-id user)
      (get-customer ctx (:user/stripe-customer-id user))

      ;; なければ Stripe 側を検索し、あれば再利用、なければ新規作成
      :else
      (let [existing (find-customer-by-email ctx email)
            customer (or existing (create-customer ctx email))]
        (if (:id customer)
          (do
            (biff/submit-tx ctx
                            [{:db/doc-type :user
                              :xt/id user-id
                              :db/op :update
                              :user/stripe-customer-id (:id customer)}])
            (if existing
              (log/info "Linked existing Stripe Customer:" (:id customer) "for user:" user-id)
              (log/info "Created new Stripe Customer:" (:id customer) "for user:" user-id))
            customer)
          (do
            (log/error "Failed to create Stripe Customer:" customer)
            nil))))))

(comment
  (require '[repl])
  (def ctx (repl/get-context))

  ;; 2. alice のユーザー ID を取得
  (def alice-user-id
    (let [{:keys [biff/db]} ctx]
      (biff/lookup-id db :user/email "alice@example.com")))

  (get-or-create-customer ctx alice-user-id)

  (let [{:keys [biff/db]} (repl/get-context)]
    (biff/lookup db :user/email "alice@example.com"))


  (get-or-create-customer ctx (random-uuid))
  :rcf)


(defn create-checkout-session
  "Checkout Session を作成"
  [{:keys [biff/secret biff/base-url]} {:keys [amount customer-id]}]
  (let [secret-key (secret :stripe/secret-key)]
    (stripe-request
     {:method :post
      :endpoint "/checkout/sessions"
      :params (cond-> {:mode "payment"
                       "line_items[0][price_data][currency]" "jpy"
                       "line_items[0][price_data][unit_amount]" amount
                       "line_items[0][price_data][product_data][name]" "ご寄付"
                       "line_items[0][quantity]" 1
                       :success_url (str base-url "/donate/complete?session_id={CHECKOUT_SESSION_ID}")
                       :cancel_url (str base-url "/donate")}
                customer-id (assoc :customer customer-id))
      :secret-key secret-key})))


(comment
  (require '[repl])
  (def ctx (repl/get-context))

  ;; Checkout Session 作成テスト（500円）
  (def session (create-checkout-session ctx {:amount 500}))
  session
  (:url session)

  ;; Customer ID 付きで作成（ログインユーザー向け）
  (def alice-user-id
    (let [{:keys [biff/db]} ctx]
      (biff/lookup-id db :user/email "alice@example.com")))

  (def customer (get-or-create-customer ctx alice-user-id))

  (def session2 (create-checkout-session ctx {:amount 1000 :customer-id (:id customer)}))
  (:url session2)

  :rcf)