(ns name-bazaar.shared.utils
  (:require
    [clojure.string :as string]
    [district0x.shared.big-number :as bn]
    [district0x.shared.utils :as d0x-shared-utils]))

(defn offering-version->type [version]
  (if (>= (bn/->number version) 100000)
    :auction-offering
    :buy-now-offering))

(defn name-label [s]
  (first (string/split s ".")))

(defn contains-number? [s]
  (boolean (re-matches #".*[0-9].*" (name-label s))))

(defn contains-special-char? [s]
  (not (boolean (re-matches #"[a-z0-9A-Z]*" (name-label s)))))

(defn contains-non-ascii? [s]
  (not (boolean (re-matches #"[ -~]*" (name-label s)))))

(defn label-length [s]
  (count (name-label s)))

(defn name-level [s]
  (count (re-seq #"\." s)))

(def offering-props [:offering/offering-registry :offering/registrar :offering/node :offering/name :offering/label-hash
                     :offering/original-owner :offering/emergency-multisig :offering/version :offering/created-on
                     :offering/new-owner :offering/price])

(defn parse-offering [offering-address offering & [{:keys [:parse-dates? :convert-to-ether?]}]]
  (when offering
    (let [offering (zipmap offering-props offering)]
      (-> offering
        (assoc :offering/address offering-address)
        (update :offering/version bn/->number)
        (assoc :offering/type (offering-version->type (:offering/version offering)))
        (update :offering/price (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/->number))
        (update :offering/created-on (if parse-dates? bn/->date-time bn/->number))
        (update :offering/new-owner #(when-not (d0x-shared-utils/zero-address? %) %))
        (assoc :offering/name-level (name-level (:offering/name offering)))
        (assoc :offering/label-length (label-length (:offering/name offering)))
        (assoc :offering/contains-number? (contains-number? (:offering/name offering)))
        (assoc :offering/contains-special-char? (contains-special-char? (:offering/name offering)))
        (assoc :offering/contains-non-ascii? (contains-non-ascii? (:offering/name offering)))))))

(def auction-offering-props [:auction-offering/end-time :auction-offering/extension-duration
                             :auction-offering/min-bid-increase :auction-offering/winning-bidder
                             :auction-offering/bid-count])

(defn parse-auction-offering [auction-offering & [{:keys [:parse-dates? :convert-to-ether?]}]]
  (when auction-offering
    (-> (zipmap auction-offering-props auction-offering)
      (update :auction-offering/end-time (if parse-dates? bn/->date-time bn/->number))
      (update :auction-offering/winning-bidder #(when-not (d0x-shared-utils/zero-address? %) %))
      (update :auction-offering/extension-duration bn/->number)
      (update :auction-offering/min-bid-increase (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/->number))
      (update :auction-offering/bid-count bn/->number))))

(defn parse-offering-requests-counts [nodes counts]
  (zipmap nodes (map #(hash-map :offering-request/requesters-count (bn/->number %)) counts)))

(def ens-record-props [:ens.record/owner :ens.record/resolver :ens.record/ttl])

(defn parse-ens-record [node ens-record & [{:keys [:parse-dates?]}]]
  (when ens-record
    (-> (zipmap ens-record-props ens-record)
      (update :ens.record/ttl bn/->number)
      (assoc :ens.record/node node))))

(def registrar-entry-states
  {0 :registrar.entry.state/open
   1 :registrar.entry.state/auction
   2 :registrar.entry.state/owned
   3 :registrar.entry.state/forbidden
   4 :registrar.entry.state/reveal
   5 :registrar.entry.state/not-yet-available})

(def registrar-entry-props [:registrar.entry/state :registrar.entry.deed/address :registrar.entry/registration-date
                            :registrar.entry/value :registrar.entry/highest-bid])

(defn parse-registrar-entry [entry & [{:keys [:parse-dates? :convert-to-ether?]}]]
  (when entry
    (-> (zipmap registrar-entry-props entry)
      (update :registrar.entry/state (comp registrar-entry-states bn/->number))
      (update :registrar.entry/registration-date (if parse-dates? bn/->date-time bn/->number))
      (update :registrar.entry/value bn/->number)
      (update :registrar.entry/highest-bid (if convert-to-ether? d0x-shared-utils/wei->eth->num bn/->number)))))


