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

(ns slackbot.routes.app-auth
  (:require
   [crypto.random :as rnd]
   [ring.util.codec :as codec]
   [ring.util.response :as response]
   [taoensso.timbre :as timbre]
   [slackbot.config :as config]
   [slackbot.database.teams :as teams]
   [slackbot.slack :as slack]))

(def requested-scopes
  ["commands"
   "emoji:read"
   "mpim:read"
   "mpim:write"
   "mpim:history"
   "im:read"
   "im:write"
   "im:history"
   "groups:read"
   "groups:write"
   "groups:history"
   "chat:write"
   "channels:read"
   "channels:write"
   "channels:history"])

(defn install
  "Generate a link to directly install this application on a Slack workspace."
  [{tx :slackbot.database/tx}]
  (let [state (rnd/base64 64)
        _     (teams/start-oauth tx {:state state})

        scopes (->> requested-scopes
                    (interpose \space)
                    (apply str))

        host   "https://slack.com/oauth/authorize"
        params (codec/form-encode {:client_id    (config/config [:slack :client-id])
                                   :redirect_url (config/config [:slack :redirect-url])
                                   :scope        scopes
                                   :state        state})

        redirect-url (str host "?" params)]
    (-> (response/response nil)
        (response/header "Location" redirect-url)
        (response/status 302))))

(defn- create-new-workspace
  "Create a new workspace record in the database and clean up the
  state generated to create it."
  [tx state {:keys [team_id access_token app_user_id]}]
  (if-let [n (teams/new-workspace tx {:workspace_id       team_id
                                      :oauth_access_token access_token
                                      :app_user_id        app_user_id})]
    (do
      (teams/delete-oauth tx {:state state})
      (-> (response/response nil)
          (response/status 204)))
    (do
      (timbre/error {:message "Unable to save new workspace/team"})
      (-> (response/response {:message "Unable to save new workspace/team"})
          (response/status 500)))))

(defn- request-oauth-token
  "Request an OAuth token from Slack and, if successful, create a new
  workspace record."
  [tx state code]
  (let [{{:keys [ok error] :as body} :body}
        @(slack/oauth-access {:code         code
                              :redirect_url (config/config [:slack :redirect-url])})]
    (if (true? ok)
      (create-new-workspace tx state body)
      (do
        (timbre/error {:message "Error message received from Slack"
                       :error   error})
        (-> (response/response {:message "Error message received from Slack"})
            (response/status 500))))))

(defn authorize
  "Complete the OAuth authorization process for a workspace.

  Take the code received in this request, submit it for a long-term OAuth
  token from the Slack API and save that token alongside the team ID and a
  few other details in the database."
  [{{:keys [code state error]} :query-params tx :slackbot.database/tx}]
  (if (= "access_denied" error)
    (do
      (timbre/error {:message "Access denied authenticating workspace"
                     :error   error})
      (-> (response/response nil)
          (response/status 204)))
    (if-let [state (teams/oauth-state tx {:state state})]
      (request-oauth-token tx state code)
      (do
        (timbre/error {:message "Received invalid state"
                       :state   state})
        (response/bad-request {:message "Received invalid state"
                               :state   state})))))
