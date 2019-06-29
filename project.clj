(defproject slackbot "0.1.0-SNAPSHOT"
  :description "Slack app and bot server"
  :url "https://github.com/chrisrink10/slackbot-clj"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.cli "0.3.5"]

                 ;; Database dependencies
                 [funcool/clojure.jdbc "0.9.0"]
                 [hikari-cp "2.5.0"]
                 [com.layerware/hugsql "0.4.9"]
                 [com.layerware/hugsql-adapter-clojure-jdbc "0.4.9"]
                 [ragtime "0.7.2"]
                 [org.xerial/sqlite-jdbc "3.23.1"]

                 ;; HTTP dependencies
                 [org.immutant/web "2.1.10"
                  :exclusions [ch.qos.logback/logback-classic]]
                 [metosin/muuntaja "0.6.0-alpha1"
                  :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [metosin/reitit "0.1.3"
                  :exclusions [com.fasterxml.jackson.core/jackson-core
                               com.fasterxml.jackson.core/jackson-databind]]
                 [ring/ring-core "1.7.0-RC1"]
                 [ring/ring-codec "1.1.1"]

                 ;; Slack Client
                 [funcool/httpurr "1.1.0"]
                 [metosin/jsonista "0.2.1"]

                 ;; Logging
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.10"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]

                 ;; Utilities
                 [aleph "0.4.6"]
                 [cprop "0.1.11"]
                 [crypto-random "1.2.0"]
                 [expound "0.7.1"]
                 [funcool/promesa "1.9.0"]
                 [medley "1.0.0"]
                 [mount "0.1.12"]]

  :profiles {:uberjar {:aot            [slackbot.core]
                       :resource-paths ["env/prod/resources"]}
             :dev     {:dependencies   [[org.clojure/tools.namespace "0.2.11"]
                                        [org.clojure/tools.trace "0.7.9"]]
                       :plugins        [[jonase/eastwood "0.3.5"]
                                        [lein-bikeshed "0.5.2"]
                                        [lein-kibit "0.1.6"]
                                        [lein-nvd "1.1.1"]]
                       :bikeshed       {:long-lines false}
                       :eastwood       {:config-files ["resources/eastwood_config.clj"]}
                       :source-paths   ["env/dev/src"]
                       :resource-paths ["env/dev/resources"]
                       :repl-options   {:init-ns slackbot-dev
                                        :init    (set! *print-length* 50)}}}

  :source-paths ["src/clojure"]
  :resource-paths ["resources"]
  :target-path "target/%s"

  :main slackbot.core)
