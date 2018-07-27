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

(ns slackbot.routes.slash-command
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [ring.util.response :as response]
   [reitit.ring :as ring]
   [taoensso.timbre :as timbre]
   [slackbot.slack :as slack]
   [slackbot.routes.slash-command.karma :as slash.karma]
   [slackbot.routes.slash-command.stinkypinky :as slash.stinkypinky]))

(s/def :slackbot.slash-command/channel_id string?)
(s/def :slackbot.slash-command/team_id string?)
(s/def :slackbot.slash-command/text string?)

(defmulti slash-command :command)
(defmethod slash-command "/cooltext" [_]
  (s/keys :req-un [:slackbot.slash-command/channel_id
                   :slackbot.slash-command/team_id
                   :slackbot.slash-command/text]))
(defmethod slash-command "/give-karma" [_]
  (s/keys :req-un [:slackbot.slash-command/channel_id
                   :slackbot.slash-command/team_id
                   :slackbot.slash-command/text]))
(defmethod slash-command "/sp" [_]
  (s/keys :req-un [:slackbot.slash-command/channel_id
                   :slackbot.slash-command/team_id
                   :slackbot.slash-command/text]))

(s/def ::command (s/multi-spec slash-command :command))

(defmulti handle
  (fn [{{:keys [command]} :body-params}]
    command))

(defmethod handle "/karma"
  [{body-params :body-params
    tx          :slackbot.database/tx
    token       :slackbot.slack/oauth-access-token}]
  (slash.karma/handle-karma body-params tx token))

(defmethod handle "/sp"
  [{body-params :body-params
    tx          :slackbot.database/tx
    token       :slackbot.slack/oauth-access-token}]
  (slash.stinkypinky/handle-stinky-pinky body-params tx token))

(defmethod handle "/cooltext"
  [{{:keys [team_id channel_id text]} :body-params
    token                             :slackbot.slack/oauth-access-token}]
  (let [cooltext (as-> text $
                   (str/replace $ #"\s" "")
                   (interpose \space $)
                   (apply str $)
                   (str "`" $ "`"))]
    (slack/send-message token
                        {:channel channel_id
                         :text    cooltext}))
  (-> (response/response nil)
      (response/status 204)))

(defmethod handle :default
  [{{:keys [command] :as body} :body-params}]
  (-> {:response_type "ephemeral"
       :text          (str "Unsupported slash command: `" command "`")}
      (response/bad-request)))
