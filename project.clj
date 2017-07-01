(defproject name-bazaar "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cljs-react-material-ui "0.2.43"]
                 [day8.re-frame/async-flow-fx "0.0.6"]
                 [district0x "0.1.6"]
                 [lein-doo "0.1.7"]
                 [madvas/reagent-patched "0.6.1" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [medley "0.8.3"]
                 [org.clojure/clojurescript "1.9.227"]
                 [print-foo-cljs "2.0.3"]
                 [re-frame "0.9.4" :exclusions [reagent]]
                 [cljsjs/solidity-sha3 "0.4.1-0"]]

  :plugins [[lein-auto "0.1.2"]
            [lein-cljsbuild "1.1.4"]
            [lein-shell "0.5.0"]
            [deraen/lein-less4j "0.5.0"]
            [lein-doo "0.1.7"]
            [lein-npm "0.6.2"]]

  :npm {:dependencies [[eth-ens-namehash "2.0.0"]
                       [ethereumjs-testrpc "3.0.3"]
                       [solc "0.4.11"]
                       [source-map-support "0.4.0"]
                       [web3 "0.19.0"]
                       [ws "2.0.1"]]}

  :min-lein-version "2.5.3"

  :source-paths ["src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:server-port 4544}

  :auto {"compile-solidity" {:file-pattern #"\.(sol)$"
                             :paths ["resources/public/contracts/src"]}}

  :aliases {"compile-solidity" ["shell" "./compile-solidity.sh"]}

  :less {:source-paths ["resources/public/less"]
         :target-path "resources/public/css"
         :target-dir "resources/public/css"
         :source-map true
         :compression true}

  :profiles {:dev
             {:dependencies [[org.clojure/clojure "1.8.0"]
                             [binaryage/devtools "0.9.4"]
                             [com.cemerick/piggieback "0.2.1"]
                             [figwheel-sidecar "0.5.10"]
                             [org.clojure/tools.nrepl "0.2.13"]]
              :plugins [[lein-figwheel "0.5.10"]]
              :source-paths []
              :resource-paths ["resources"]
              :cljsbuild {:builds [{:id "dev"
                                    :source-paths ["src/cljs"]
                                    :figwheel {:on-jsload "name-bazaar.core/mount-root"}
                                    :compiler {:main "name-bazaar.core"
                                               :output-to "resources/public/js/compiled/app.js"
                                               :output-dir "resources/public/js/compiled/out"
                                               :asset-path "js/compiled/out"
                                               :source-map-timestamp true
                                               :preloads [print.foo.preloads.devtools]
                                               :closure-defines {goog.DEBUG true}
                                               :external-config {:devtools/config {:features-to-install :all}}}}
                                   {:id "min"
                                    :source-paths ["src/cljs"]
                                    :compiler {:main "name-bazaar.core"
                                               :output-to "resources/public/js/compiled/app.js"
                                               :optimizations :advanced
                                               :closure-defines {goog.DEBUG false}
                                               :pretty-print false
                                               :pseudo-names false}}
                                   {:id "test-fig"
                                    :source-paths ["src/cljs" "test"]
                                    :figwheel true
                                    :compiler {:main "name-bazaar.cmd"
                                               :output-to "test-fig-compiled/name-bazaar-tests.js",
                                               :output-dir "test-fig-compiled",
                                               :target :nodejs,
                                               :optimizations :none,
                                               :verbose false
                                               :source-map true}}]}}}

  )