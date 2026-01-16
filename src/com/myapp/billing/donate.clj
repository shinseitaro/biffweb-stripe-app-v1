(ns com.myapp.billing.donate
  (:require [com.biffweb :as biff]
            [com.myapp.ui :as ui]
            [com.myapp.settings :as settings]
            [com.myapp.billing.stripe :as stripe]
            [ring.util.response :as response]))


(defn donate-page [{:keys [params] :as ctx}]
  (let [error (:error params)]
    (ui/page
     ctx
     [:.max-w-md.mx-auto
      [:h1.text-2xl.font-bold.mb-6 "ご寄付"]

      (when error
        [:.bg-red-100.text-red-700.p-4.rounded.mb-4
         error])

      ;; biff/form は CSRF トークンを自動挿入し、POST を送信
      (biff/form
       {:action "/donate"
        :method "POST"
        :class "space-y-4"}

       [:div
        [:label.block.text-sm.font-medium.mb-1 {:for "amount"} "金額を選択"]
        [:select#amount.w-full.p-2.border.rounded
         {:name "amount"}
         [:option {:value "500"} "500円"]
         [:option {:value "1000"} "1,000円"]
         [:option {:value "3000"} "3,000円"]
         [:option {:value "5000"} "5,000円"]
         [:option {:value "10000"} "10,000円"]]]

       [:button.w-full.bg-blue-600.text-white.py-2.px-4.rounded.hover:bg-blue-700
        {:type "submit"}
        "Stripe で支払う"])])))


(defn create-checkout-and-redirect [{:keys [session params] :as ctx}]
  (let [amount (some-> (:amount params) parse-long)]
    (if (and amount (>= amount 100))
      ;; ログイン中ならユーザーの Customer を取得または作成
      (let [customer-id (when-let [uid (:uid session)]
                          (:id (stripe/get-or-create-customer ctx uid)))
            result (stripe/create-checkout-session ctx
                                                   {:amount amount
                                                    :customer-id customer-id})]
        (if-let [checkout-url (:url result)]
          ;; Stripe Checkout ページにリダイレクト
          (response/redirect checkout-url)
          ;; エラー時は寄付ページに戻る
          (response/redirect (str "/donate?error=" (java.net.URLEncoder/encode
                                                    "決済の準備に失敗しました" "UTF-8")))))
      ;; バリデーションエラー
      (response/redirect (str "/donate?error=" (java.net.URLEncoder/encode
                                                "金額を選択してください" "UTF-8"))))))

(defn complete-page [{:keys [params] :as ctx}]
  (let [session-id (:session_id params)]
    (ui/page
     ctx
     [:.max-w-md.mx-auto.text-center
      [:h1.text-2xl.font-bold.text-green-600.mb-4 "決済が完了しました"]
      [:p.mb-4 "ご支援ありがとうございます！"]
      (when session-id
        [:p.text-sm.text-gray-600.mb-6 "セッションID: " session-id])
      [:a.text-blue-600.hover:underline {:href "/"} "トップページに戻る"]])))

(def module
  {:routes [["/donate" {:get donate-page
                        :post create-checkout-and-redirect}]
            ["/donate/complete" {:get complete-page}]]})