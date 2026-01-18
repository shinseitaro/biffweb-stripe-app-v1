(ns com.myapp.schema)

(def schema
  {:user/id :uuid
   :user [:map {:closed true}
          [:xt/id                     :user/id]
          [:user/email                :string]
          [:user/joined-at            inst?]
          [:user/stripe-customer-id {:optional true} :string]]

   :payment/id :uuid
   :payment [:map {:closed true}
             [:xt/id :payment/id]
             [:payment/stripe-session-id {:optional true} :string]
             [:payment/stripe-payment-intent-id  :string]
             [:payment/amount :int]
             [:payment/currency :string]
             [:payment/status :string]
             [:payment/user {:optional true} [:or :user/id :nil]]
             [:payment/created-at inst?]]



   :webhook-event/id :uuid
   :webhook-event [:map {:closed true}
                   [:xt/id :webhook-event/id]
                   [:webhook-event/stripe-id :string]
                   [:webhook-event/type :string]
                   [:webhook-event/processed-at inst?]]})



(def module
  {:schema schema})
