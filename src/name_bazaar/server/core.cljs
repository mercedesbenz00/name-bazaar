(ns name-bazaar.server.core
  (:require
    [cljs.core.async :refer [<! >! chan take!]]
    [cljs.nodejs :as nodejs]
    [district0x.server.api-server :as api-server]
    [district0x.server.effects :as d0x-effects]
    [district0x.server.logging :as d0x-logging]
    [district0x.server.state :as state :refer [*server-state*]]
    [name-bazaar.server.api]
    [name-bazaar.server.db-sync :as db-sync]
    [name-bazaar.server.emailer.listeners :as listeners]
    [name-bazaar.server.watchdog :as watchdog]
    [name-bazaar.shared.smart-contracts :refer [smart-contracts]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(nodejs/enable-util-print!)

(def Web3 (nodejs/require "web3"))
(set! js/Web3 Web3)

(def ecc (nodejs/require "eccjs"))
(set! js/ecc ecc)

(defn -main [& _]
  (d0x-effects/load-config! state/default-config)
  (d0x-logging/setup! (state/config :logging))
  (go
    (d0x-effects/create-web3! {:port (state/config :mainnet-port)})
    (d0x-effects/load-smart-contracts! smart-contracts)
    (api-server/start! (state/config :api-port))
    (<! (d0x-effects/load-my-addresses!))
    (watchdog/start-syncing!)))

(set! *main-cli-fn* -main)
