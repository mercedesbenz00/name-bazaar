(ns name-bazaar.ui.subs.registration-bids-subs
  (:require [cljs.core.match :refer-macros [match]]
            [cljs-time.core :as t]
            [name-bazaar.shared.constants :as constants]
            [name-bazaar.ui.subs.registrar-subs :as registrar-subs]
            [name-bazaar.ui.utils :as nb-ui-utils]
            [re-frame.core :as re-frame]))

(defn- query-state
  "Time based state query.
  Drop-in replacement for querying the registrar contract."
  [reveal-period registration-date now highest-bid]
  (if (nil? registration-date)
    :name-bazaar-registrar.entry.state/open
    (match [(t/after? registration-date now) (t/after? (registrar-subs/query-end-bidding-date registration-date reveal-period) now) (= 0 highest-bid)]
           [false _ true] :name-bazaar-registrar.entry.state/open
           [true true _] :name-bazaar-registrar.entry.state/auction
           [true false _] :name-bazaar-registrar.entry.state/reveal
           [false _ false] :name-bazaar-registrar.entry.state/owned)))

(defn query-registrar-auction-state [db active-address label-hash]
  (let [{:keys [:name-bazaar-registrar.entry/registration-date :name-bazaar-registrar.entry/highest-bid
                :name-bazaar-registrar.entry/value :name-bazaar-registrar.entry.deed/owner]} (get-in db [:name-bazaar-registrar/entries label-hash])
        {:keys [:name-bazaar-registrar/label :name-bazaar-registrar/bid-salt :name-bazaar-registrar/bid-value
                :registration-bids/bid-unsealed?] :as bid} (get-in db [:registration-bids active-address label-hash])
        {:keys [:reveal-period]} (get-in db [:config])
        state (query-state reveal-period registration-date (:now db) highest-bid)
        loading? (-> (get-in db [:name-bazaar-registrar/entries label-hash])
                     nb-ui-utils/registrar-entry-deed-loaded?
                     not)
        empty? (or (nil? state) (= constants/empty-label-hash label-hash))
        open? (= :name-bazaar-registrar.entry.state/open state)
        auction? (= :name-bazaar-registrar.entry.state/auction state)
        reveal? (= :name-bazaar-registrar.entry.state/reveal state)
        owned? (= :name-bazaar-registrar.entry.state/owned state)
        finalized? (not (= 0 value))]
    (vector (match [loading? empty? open? auction? reveal? owned? (nil? bid-value) bid-unsealed? (= active-address owner) finalized?]
                   [_ true _ _ _ _ _ _ _ _] :name-bazaar-registrar.entry.state/empty-name
                   [true false  _ _ _ _ _ _ _ _] :name-bazaar-registrar.entry.state/loading
                   [false false true _ _ _ _ _ _ _] state
                   [false false _ true _ _ true _ _ _] :name-bazaar-registrar.entry.state/auction-no-user-made-bid
                   [false false _ true _ _ false _ _ _] :name-bazaar-registrar.entry.state/auction-user-made-bid
                   [false false _ _ true _ true _ _ _] :name-bazaar-registrar.entry.state/reveal-phase-no-user-made-bid
                   [false false _ _ true _ false false false _] :name-bazaar-registrar.entry.state/reveal-phase-user-made-bid
                   [false false _ _ true _ false _ true _] :name-bazaar-registrar.entry.state/reveal-phase-user-winning
                   [false false _ _ true _ false true false _] :name-bazaar-registrar.entry.state/reveal-phase-user-outbid
                   [false false _ _ _ true _ _ true false] :name-bazaar-registrar.entry.state/owned-phase-user-owner-not-finalized
                   [false false _ _ _ true _ _ true true] :name-bazaar-registrar.entry.state/owned-phase-user-owner
                   [false false _ _ _ true _ _ false true] :name-bazaar-registrar.entry.state/owned-phase-different-owner
                   [false false _ _ _ true _ _ false false] :name-bazaar-registrar.entry.state/owned-phase-different-owner-not-finalized
                   :else state)
            state)))

(re-frame/reg-sub
 :registration-bids/user-bids
 (fn [db _]
   (get-in db [:registration-bids (:active-address db)])))

(re-frame/reg-sub
 :registration-bid
 (fn [db [_ label-hash]]
   (get-in db [:registration-bids (:active-address db) label-hash])))

(re-frame/reg-sub
 :registration-bids/bid-unsealed?
 (fn [[_ label-hash]]
   [(re-frame/subscribe [:registration-bid label-hash])])
 (fn [[{:keys [:name-bazaar-registrar/bid-unsealed?] :as bid}]]
   bid-unsealed?))

(re-frame/reg-sub
 :registration-bids/state-count
 (fn [db [_ state]]
   (let [active-address (:active-address db)]
     (reduce (fn [m [label-hash {:keys [:name-bazaar-registrar/label ] :as bid}]]
               (cond
                 (not state)
                 m
                 (-> state
                     (= (first (query-registrar-auction-state db active-address label-hash))))
                 (update m :count inc)
                 :else m))
             {:registration-bids/state state :count nil}
             (get-in db [:registration-bids active-address])))))

(re-frame/reg-sub
 :registration-bids/user-bids-by-importance
 (fn [db [_ fsm]]
   (let [active-address (:active-address db)
         user-bids (get-in db [:registration-bids active-address])]
     (sort (fn [{s1 :registration-bids/state} {s2 :registration-bids/state}]
             (compare (get-in fsm [s2 :weight])
                      (get-in fsm [s1 :weight])))
           (reduce-kv (fn [res label-hash {:keys [registrar/label]}]
                        (conj res {:title label :registration-bids/state (first (query-registrar-auction-state db active-address label-hash))}))
                      []
                      user-bids)))))

(re-frame/reg-sub
 :name-bazaar-registrar/auction-state
 (fn [db [_ label-hash]]
   (query-registrar-auction-state db (:active-address db) label-hash)))
