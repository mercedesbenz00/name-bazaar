(ns district0x.db
  (:require [cljs.spec.alpha :as s]
            [district0x.utils :as u]
            [re-frame.core :refer [dispatch]]))

(s/def ::load-node-addresses? boolean?)
(s/def ::web3 (complement nil?))
(s/def ::node-url string?)
(s/def ::server-url string?)
(s/def ::contracts-not-found? boolean?)
(s/def ::handler keyword?)
(s/def ::route-params (s/map-of keyword? (some-fn number? string?)))
(s/def ::active-page (s/keys :req-un [::handler] :opt-un [::route-params]))
(s/def ::window-width-size int?)
(s/def ::open? boolean?)
(s/def ::message string?)
(s/def ::on-request-close fn?)
(s/def ::auto-hide-duration int?)
(s/def ::modal boolean?)
(s/def ::title string?)
(s/def ::snackbar (s/keys :req-un [::open? ::message ::on-request-close ::auto-hide-duration]))
(s/def ::dialog (s/keys :req-un [::open? ::title ::modal]))
(s/def ::name string?)
(s/def ::address string?)
(s/def ::bin string?)
(s/def ::abi array?)
(s/def ::setter? boolean?)
(s/def ::smart-contracts (s/map-of keyword? (s/keys :req-un [::name] :opt-un [::setter? ::address ::bin ::abi])))
(s/def ::my-addresses (s/coll-of string?))
(s/def ::active-address (s/nilable string?))
(s/def ::active-address ::sender)
(s/def ::conversion-rates (s/map-of number? number?))
(s/def ::load-conversion-rates-interval (s/nilable int?))
(s/def ::blockchain-connection-error? boolean?)
(s/def ::balances (s/map-of u/address? (s/map-of keyword? u/not-neg?)))
(s/def ::ui-disabled? boolean?)

(s/def ::contract-address u/address?)
(s/def ::errors (s/coll-of keyword?))
(s/def ::data map?)
(s/def ::contract-address-form-id (s/keys :req-un [::contract-address]))
(s/def ::nil-form-id nil?)
(s/def ::form-key keyword?)
(s/def ::form (s/keys :opt-un [::errors ::data]))
(s/def ::form-id (s/nilable map?))


(s/def ::forms-by-contract-address (s/map-of ::contract-address-form-id ::form))

(s/def ::transaction-id u/sha3?)
(s/def ::transaction-id-list (s/coll-of ::transaction-id :kind list?))
(s/def ::transactions (s/map-of ::transaction-id map?))
(s/def ::transactions-latest ::transaction-id-list)
(s/def ::transactions-by-form (s/map-of ::form-key
                                        (s/map-of ::sender
                                                  (s/map-of ::form-id ::transaction-id-list))))

(s/def ::order-by-dir (partial contains? #{:asc :desc}))
(s/def ::order-by (s/map-of any? ::order-by-dir))
(s/def ::offset integer?)
(s/def ::limit integer?)
(s/def ::infinite-scroll (s/keys :opt-un [::offset ::limit]))
(s/def ::list (s/keys :opt-un [::ids ::loading? ::infinite-scroll]))

(s/def ::items (s/coll-of any?))

(s/def ::contract-method-args-order (s/map-of ::form-key vector?))
(s/def ::contract-method-wei-args set?)
(s/def ::form-default-params (s/map-of ::form-key (s/map-of keyword? any?)))
(s/def ::form-tx-opts (s/map-of ::form-key (s/map-of keyword? any?)))

(s/def :district0x-emails/set-email (s/map-of :district0x.db/nil-form-id :district0x.db/form))

(s/def ::db (s/keys :req-un [::active-address
                             ::blockchain-connection-error?
                             ::contracts-not-found?
                             ::dialog
                             ::my-addresses
                             ::node-url
                             ::server-url
                             ::smart-contracts
                             ::snackbar
                             ::web3
                             ::ui-disabled?
                             ::transactions
                             ::transactions-latest
                             ::transactions-by-form
                             ::contract-method-args-order
                             ::contract-method-wei-args
                             ::form-default-params
                             ::form-tx-opts]
                    :opt-un [::active-page
                             ::balances
                             ::conversion-rates
                             ::load-conversion-rates-interval
                             ::load-node-addresses?
                             :district0x-emails/set-email]))

(def default-db
  {:web3 nil
   :contracts-not-found? false
   :window-width-size (u/get-window-width-size js/window.innerWidth)
   :ui-disabled? false
   :snackbar {:open? false
              :message ""
              :auto-hide-duration 5000
              :on-request-close #(dispatch [:district0x.snackbar/close])}
   :dialog {:open? false
            :modal false
            :title ""
            :actions []
            :body ""
            :on-request-close #(dispatch [:district0x.dialog/close])}
   :smart-contracts {}
   :my-addresses []
   :active-address nil
   :blockchain-connection-error? false
   :conversion-rates {}
   :balances {}
   :transactions {}
   :transactions-latest '()
   :transactions-by-form {}
   :contract-method-args-order {}
   :contract-method-wei-args #{}
   :form-default-params {}
   :form-tx-opts {}})




