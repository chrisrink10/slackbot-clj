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

(ns slackbot.slack
  (:require
   [httpurr.status :as s]
   [medley.core :refer [assoc-some]]
   [promesa.core :as p]
   [ring.util.codec :as codec]
   [taoensso.timbre :as timbre]
   [slackbot.config :as config]
   [slackbot.http :as http]))

(defn auth-header
  "Generate an Authorization header befitting a Slack POST request."
  [token]
  (str "Bearer " token))

(defn oauth-access-auth-header
  "Generate an Authorization header for OAuth access methods."
  []
  (let [{:keys [client-id client-secret]} (config/config [:slack])]
    (->> (str client-id ":" client-secret)
         (.getBytes)
         (codec/base64-encode)
         (str "Basic "))))

(defn post-slack
  "Post a JSON body to Slack at the given URI."
  ([token uri body]
   (post-slack token uri body {}))
  ([token uri body headers]
   (http/post uri
              {:headers (merge
                         {"authorization" (auth-header token)
                          "content-type"  "application/json"}
                         headers)
               :body    body})))

(defn oauth-access
  "Request an OAuth access token for the current team.

  This should only happen the first time a member of the team authorizes
  this app for their workspace.

  This method doesn't use the standard `post-slack` method above because
  it actually uses the client ID and client secret as a HTTP Basic auth
  header, since we do not yet have an OAuth access token for requests
  pertaining a specific team or workspace."
  [{:keys [code redirect-uri]}]
  (-> (http/post "https://slack.com/api/oauth.access"
                 {:headers {"authorization" (oauth-access-auth-header)
                            "content-type"  "application/x-www-form-urlencoded"}
                  :body    (assoc-some {:code code} :redirect-uri redirect-uri)})
      (p/then (fn [{{:keys [ok]} :body :as response}]
                (cond
                  (and (s/success? response) (true? ok))
                  (do
                    (timbre/debug {:message  "Received post oauth.access response from Slack"
                                   :response response})
                    response)

                  :else
                  (timbre/error {:message "Error occurred requesting OAuth access token"
                                 :error   (:body response)})))) ))

(defn send-message
  "Send a Slack message.

  Callers need to supply the OAuth access token for the workspace they
  are sending messages to."
  [token {:keys [channel text attachments]}]
  (-> (post-slack token
                  "https://slack.com/api/chat.postMessage"
                  (assoc-some {:channel channel
                               :text    text}
                              :attachments attachments))
      (p/then (fn [{{:keys [ok]} :body :as response}]
                (cond
                  (and (s/success? response) (true? ok))
                  (timbre/debug {:message  "Received postMessage response from Slack"
                                 :response response})

                  :else
                  (timbre/error {:message     "Could not post response message"
                                 :error       (:body response)
                                 :channel     channel
                                 :text        text
                                 :attachments attachments})))) ))
