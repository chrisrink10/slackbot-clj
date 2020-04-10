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

(ns slackbot.routes.slack-event
  (:require
   [ring.util.response :as response]
   [taoensso.timbre :as timbre]
   [slackbot.karma :as karma]))

(defmulti handle-event-callback
  (fn [{{{:keys [type channel_type subtype]} :event} :body-params}]
    (let [event-type (keyword type)]
      (if (and (= :message event-type) (nil? subtype))
        :message
        [event-type (keyword channel_type) (keyword subtype)]))))

(defmethod handle-event-callback :message
  [{{{:keys [channel text]} :event team-id :team_id} :body-params
    tx                                               :slackbot.database/tx
    token                                            :slackbot.slack/oauth-access-token}]
  (karma/process-karma tx token team-id channel text)
  (-> (response/response nil)
      (response/status 200)))

(defmethod handle-event-callback :default
  [{{{event-type :type channel-type :channel_type} :event} :body-params}]
  (timbre/info {:message      "Received unsupported event callback"
                :event-type   event-type
                :channel-type channel-type})
  (-> (response/response nil)
      (response/status 200)))

(defmulti handle
  (fn [{{event-type :type} :body-params}]
    (keyword event-type)))

(defmethod handle :url_verification
  [{{:keys [challenge]} :body-params}]
  (-> challenge
      (response/response)
      (response/status 200)
      (response/content-type "text/plain")))

(defmethod handle :event_callback
  [req]
  (handle-event-callback req))

(defmethod handle :default
  [{{event-type :type} :body-params}]
  (timbre/info {:message      "Received unsupported event"
                :event-type   event-type})
  (-> (response/response nil)
      (response/status 200)))
