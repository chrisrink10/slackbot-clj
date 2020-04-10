(defproject slackbot "0.1.0-SNAPSHOT"
  :description "Slack app and bot server"
  :url "https://github.com/chrisrink10/slackbot-clj"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.cli "0.3.5"]

                 ;; Database dependencies
                 [funcool/clojure.jdbc "0.9.0"]
                 [hikari-cp "2.7.1"]
                 [com.layerware/hugsql "0.4.9"]
                 [com.layerware/hugsql-adapter-clojure-jdbc "0.4.9"]
                 [ragtime "0.8.0"]
                 [org.xerial/sqlite-jdbc "3.28.0"]

                 ;; HTTP dependencies
                 [org.immutant/web "2.1.10"
                  :exclusions [ch.qos.logback/logback-classic]]
                 [metosin/muuntaja "0.6.4"
                  :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [metosin/reitit "0.3.9"
                  :exclusions [mvxcvi/arrangement
                               mvxcvi/puget
                               com.fasterxml.jackson.core/jackson-databind
                               com.fasterxml.jackson.core/jackson-core]]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-codec "1.1.2"]

                 ;; Slack Client
                 [funcool/httpurr "1.1.0"]
                 [metosin/jsonista "0.2.3"]

                 ;; Logging
                 [com.taoensso/timbre "4.10.0"]
                 [com.fzakaria/slf4j-timbre "0.3.13"]
                 [org.slf4j/log4j-over-slf4j "1.7.26"]
                 [org.slf4j/jul-to-slf4j "1.7.26"]
                 [org.slf4j/jcl-over-slf4j "1.7.26"]

                 ;; Utilities
                 [aleph "0.4.6"]
                 [cprop "0.1.13"]
                 [crypto-random "1.2.0"]
                 [expound "0.7.2"]
                 [funcool/promesa "2.0.1"
                  :exclusions [com.google.code.findbugs/jsr305
                               com.google.errorprone/error_prone_annotations
                               org.clojure/tools.reader]]
                 [medley "1.2.0"]
                 [mount "0.1.16"]]

  :profiles {:uberjar {:aot            [slackbot.core]
                       :resource-paths ["env/prod/resources"]}
             :dev     {:dependencies   [[org.clojure/tools.namespace "0.2.11"]
                                        [org.clojure/tools.trace "0.7.9"]]
                       :plugins        [[clj-kondo "2020.04.05"]
                                        [jonase/eastwood "0.3.11"]
                                        [lein-bikeshed "0.5.2"]
                                        [lein-nvd "1.1.1"]]
                       :bikeshed       {:long-lines false}
                       :eastwood       {:config-files ["resources/eastwood_config.clj"]}
                       :source-paths   ["env/dev/src"]
                       :resource-paths ["env/dev/resources"]
                       :repl-options   {:init-ns slackbot-dev
                                        :init    (set! *print-length* 50)}}}

  :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]
            "lint"      ["with-profile" "+dev" "do" "eastwood," "bikeshed"]}

  :source-paths ["src/clojure"]
  :resource-paths ["resources"]
  :target-path "target/%s"

  :main slackbot.core)
