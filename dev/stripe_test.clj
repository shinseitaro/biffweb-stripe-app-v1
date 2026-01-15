(ns stripe-test
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [com.myapp :as main]))

(defn stripe-secret-key
  "Biff システムから Stripe シークレットキーを取得"
  []
  ((:biff/secret @main/system) :stripe/secret-key))

(comment
  ;; 設定変更後はリフレッシュ
  (com.myapp/refresh)
  (stripe-secret-key)
  :rcf)