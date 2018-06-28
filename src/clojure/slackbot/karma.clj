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

(ns slackbot.karma
  (:require
   [slackbot.database.karma :as db.karma]
   [slackbot.slack :as slack]))

(defn inc-karma-seq
  "Given a Slack message, return a seq of targets which should receive a
  karma point from the message."
  [text]
  (->> (re-seq #"(\S+)\+\+" text)
       (map second)))

(defn dec-karma-seq
  "Given a Slack message, return a seq of targets which should lose a
  karma point from the message."
  [text]
  (->> (re-seq #"(\S+)\-\-" text)
       (map second)))

(defn report-target-karma
  "Report the karma of a target to the specified channel."
  [tx token team-id channel-id target]
  (let [karma (-> (db.karma/target-karma tx {:workspace_id team-id
                                             :target       target})
                  (:karma))]
    (slack/send-message token
                        {:channel channel-id
                         :text    (str target " has " (or karma 0) " karma.")})))

(defn report-top-karma
  "Report the top n users in descending order of karma points to
  the specified channel."
  ([tx token team-id channel-id]
   (report-top-karma tx token team-id channel-id 10))
  ([tx token team-id channel-id n]
   (let [top-users (db.karma/top-karma tx
                                       {:workspace_id team-id
                                        :n            n})

         msg (->> top-users
                  (map-indexed (fn [i {:keys [target karma]}]
                                 (str (inc i) ". *" target "* has _" karma "_ karma.")))
                  (interpose \newline)
                  (apply str))]
     (slack/send-message token
                         {:channel     channel-id
                          :attachments [{:title    (str "Top " n " Users by Karma")
                                         :fallback msg
                                         :text     msg}]}))))

(defn give-karma
  "Give the target the amount of karma specified (or 1, if not specified).
  Report the change in the designated channel."
  ([tx token team-id channel-id target]
   (give-karma tx token team-id channel-id target 1))
  ([tx token team-id channel-id target amount]
   (let [{new-amount :karma} (db.karma/inc-karma tx {:workspace_id team-id
                                                     :target       target
                                                     :karma        amount})]
     (slack/send-message token
                         {:channel channel-id
                          :text    (str target " now has " new-amount " karma!")}))))

(defn take-karma
  "Take karma from the target (1, if not specified). Report the change
  in the designated channel."
  ([tx token team-id channel-id target]
   (take-karma tx token team-id channel-id target 1))
  ([tx token team-id channel-id target amount]
   (let [{new-amount :karma} (db.karma/dec-karma tx {:workspace_id team-id
                                                     :target       target
                                                     :karma        amount})]
     (slack/send-message token
                         {:channel channel-id
                          :text    (str target " now has " new-amount " karma!")}))))

(defn process-karma
  "Process all the karma additions and subtractions in a message and
  change the karma inside the database and report back to Slack the changes."
  [tx token team-id channel-id text]
  (doseq [target (inc-karma-seq text)]
    (give-karma tx token team-id channel-id target))
  (doseq [target (dec-karma-seq text)]
    (take-karma tx token team-id channel-id target)))
