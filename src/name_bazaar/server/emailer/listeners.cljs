(ns name-bazaar.server.emailer.listeners
  (:require [cljs-web3.eth :as web3-eth]
            [cljs.core.async :refer [<! >! chan]]
            [district0x.server.state :as state]
            [name-bazaar.server.contracts-api.district0x-emails :as district0x-emails]
            [name-bazaar.server.contracts-api.offering-requests :as offering-requests]
            [name-bazaar.server.db :as db]
            [district0x.server.emailer.sendgrid :as sendgrid]
            [name-bazaar.server.emailer.templates :as templates]
            [district0x.shared.config :as config]
            [district0x.shared.encryption-utils :as encryption-utils])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn on-offering-added [server-state {:keys [:offering :node :owner :version] :as args}]
  (go
    (let [[error success] (<! (offering-requests/get-request @server-state {:offering-request/node node}))]
      (if error
        (prn "An error has occured: " error)
        (let [{:keys [name :offering-request/name
                      requesters-count :offering-request/requesters-count
                      latest-round :offering-request/latest-round
                      node :offering-request/node]} success]
          (when (pos? requesters-count)
            (let [[_ addresses] (<! (offering-requests/get-requesters @server-state {:offering-request/node node
                                                                                     :offering-request/round latest-round}))]
              (for [address addresses]
                (let [[_ base64-encrypted-email] (<! (district0x-emails/get-email @server-state {:district0x-emails/address address}))]
                  (when-not (empty? (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                                                       :to-email (->> base64-encrypted-email
                                                                                      (key-utils/decode-base64)
                                                                                      (encryption-utils/decrypt (config/get-config :private-key)))
                                                                       :subject "Offering created"
                                                                       :content (templates/on-offering-added offering name)}
                                                                      #(prn "Success sending email")
                                                                      #(prn "An error has occured: " %)))))))))))))

(defn- on-auction-finalized
  [server-state original-owner winning-bidder name price]
  (go
    (let [[_ owner-encrypted-email] (<! (district0x-emails/get-email @server-state {:district0x-emails/address original-owner}))
          [_ winner-encrypted-email] (<! (district0x-emails/get-email @server-state {:district0x-emails/address winning-bidder}))]
      (when-not (empty? owner-encrypted-email)
        (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                           :to-email (->> owner-encrypted-email
                                                          (encryption-utils/decode-base64)
                                                          (encryption-utils/decrypt (config/get-config :private-key)))
                                           :subject "Auction was finalized"
                                           :content (templates/on-auction-finalized :owner offering name price)}
                                          #(prn "Success sending email")
                                          #(prn "An error has occured: " %)))
      (when-not (empty? winner-encrypted-email)
        (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                           :to-email (->> winner-encrypted-email
                                                          (encryption-utils/decode-base64)
                                                          (encryption-utils/decrypt (config/get-config :private-key)))
                                           :subject "Auction was finalized"
                                           :content (templates/on-auction-finalized :winner offering name price)}
                                          #(prn "Success sending email")
                                          #(prn "An error has occured: " %))))))

(defn- on-offering-bought [server-state original-owner name price]
  (go
    (let [[_ owner-encrypted-email] (<! (district0x-emails/get-email @server-state {:district0x-emails/address original-owner}))]
      (when-not (empty? owner-encrypted-email)
        (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                           :to-email (->> owner-encrypted-email
                                                          (encryption-utils/decode-base64)
                                                          (encryption-utils/decrypt (config/get-config :private-key)))
                                           :subject "Your offering was bought"
                                           :content (templates/on-offering-bought offering name price)}
                                          #(prn "Success sending email")
                                          #(prn "An error has occured: " %))))))

(defn on-offering-changed [server-state {:keys [:offering :version :event-type :extra-data] :as args}]
  (go
    (let [{:keys [:name :original-owner :price :end-time :winning-bidder] :as result} (<! (db/get-offering (state/db @server-state) offering))]
      (if winning-bidder
        (on-auction-finalized server-state original-owner winning-bidder name price)
        (on-offering-bought server-state original-owner name price)))))

(defn on-new-bid
  [server-state {:keys [:offering] :as args}]
  (go
    (let [{:keys [:name :original-owner :price :end-time :winning-bidder] :as result} (<! (db/get-offering (state/db @server-state) offering))
          [_ owner-encrypted-email] (<! (district0x-emails/get-email @server-state {:district0x-emails/address original-owner}))]
      (when-not (empty? owner-encrypted-email)
        (sendgrid/send-notification-email {:from-email "hello@district0x.io"
                                           :to-email (->> owner-encrypted-email
                                                          (encryption-utils/decode-base64)
                                                          (encryption-utils/decrypt (config/get-config :private-key)))
                                           :subject "Your offering was bought"
                                           :content (templates/on-new-bid offering name price)}
                                          #(prn "Success sending email")
                                          #(prn "An error has occured: " %))))))

(defn setup-listener!
  ([server-state contract-key event-key callback]
   (setup-listener! server-state contract-key event-key true nil callback))
  ([server-state contract-key event-key retrieve-events? event-type callback]
   (web3-eth/contract-call (state/instance @server-state contract-key)
                           event-key
                           (if event-type
                             {:event-type event-type}
                             {})
                           (if retrieve-events?
                             {:from-block 0 :to-block "latest"}
                             "latest")
                           (fn [error {:keys [:args] :as response}]
                             (if error
                               (prn "An error has occured: " error)
                               (callback server-state args))))))

(defn setup-event-listeners! [server-state]
  (setup-listener! server-state :offering-registry :on-offering-added on-offering-added)
  (setup-listener! server-state :offering-registry :on-offering-changed false "finalize" on-offering-changed)
  (setup-listener! server-state :offering-registry :on-offering-changed false "bid" on-new-bid))


