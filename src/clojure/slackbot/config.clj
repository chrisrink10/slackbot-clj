;; Copyright 2018 Chris Rink
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns slackbot.config
  (:require
   [clojure.spec.alpha :as s]
   [cprop.core :as cprop]
   [cprop.source :as source]
   [taoensso.timbre :as timbre]))

(s/def :slackbot.database/connection-string string?)
(s/def :slackbot.config/database (s/keys :req-un [:slackbot.database/connection-string]))

(s/def :slackbot.logging/level timbre/-levels-set)
(s/def :slackbot.config/logging (s/keys :req-un [:slackbot.logging/level]))

(s/def :slackbot.proxy/bot-api-url string?)
(s/def :slackbot.config/proxy (s/keys :opt-un [:slackbot.proxy/bot-api-url]))

(s/def :slackbot.slack/api-url string?)
(s/def :slackbot.slack/client-id string?)
(s/def :slackbot.slack/client-secret string?)
(s/def :slackbot.slack/redirect-url string?)
(s/def :slackbot.slack/verification-token string?)
(s/def :slackbot.config/slack (s/keys :req-un [:slackbot.slack/client-id
                                               :slackbot.slack/client-secret
                                               :slackbot.slack/redirect-url
                                               :slackbot.slack/verification-token]))

(s/def :slackbot.web-server/host string?)
(s/def :slackbot.web-server/port (s/int-in 1 65535))
(s/def :slackbot.web-server/web-server (s/keys :req-un [:slackbot.web-server/host
                                                        :slackbot.web-server/port]))

(s/def :slackbot.config/slackbot (s/keys :req-un [:slackbot.config/database
                                                  :slackbot.config/logging
                                                  :slackbot.config/slack
                                                  :slackbot.config/web-server]
                                         :opt-un [:slackbot.config/proxy]))

(s/def :slackbot/config (s/keys :req-un [:slackbot.config/slackbot]))

(defn read-env-config
  "Read the application configuration from the application defaults,
  environment variables, and any specified overrides.

  This function rebinds `*out*' to suppress `println' output from the
  cprop library.

  Returns the configuration formed by merging (1) the defaults, (2)
  environment variables, and (3) any overrides specified as an argument."
  [overrides]
  (binding [*out* (new java.io.StringWriter)]
    (cprop/load-config :merge [(source/from-env) overrides])))

(defn read-config
  "Read the application configuration using `read-env-config' and
  validate it against the config spec.

  Throws an exception if the spec fails. Returns the configuration
  otherwise."
  ([]
   (read-config {}))
  ([overrides]
   (let [config (-> (read-env-config overrides)
                    (select-keys [:slackbot]))]
     (if (s/valid? :slackbot/config config)
       (:slackbot config)
       (throw
        (ex-info (s/explain-str :slackbot/config config)
                 (s/explain-data :slackbot/config config)))))))

(def config-cache
  (memoize read-config))

(defn config
  "Read configuration values from the cached environment config.

  If arguments are provided, they are treated as in `get-in' to
  access sub-sections of the configuration map."
  ([]
   (config-cache))
  ([ks]
   (-> (config-cache) (get-in ks)))
  ([ks not-found]
   (-> (config-cache) (get-in ks not-found))))
