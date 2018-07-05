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

(ns slackbot.stinkypinky
  (:require
   [taoensso.timbre :as timbre]
   [slackbot.database.stinkypinky :as db.stinkypinky]
   [slackbot.slack :as slack]))

(defn guess
  "Get a Stinky Pinky guess if it is valid."
  [text]
  (some->> text (re-matches #"(\S+ \S+)") (second)))

(defn is-guess-correct?
  "Return true if the guess given was correct."
  [tx token workspace-id channel-id user-id guess]
  (let [is-guess-correct? (->> {:workspace_id workspace-id
                                :channel_id   channel-id
                                :guess        guess
                                :user_id      user-id}
                               (db.stinkypinky/check-stinky-pinky-guess tx)
                               (:guess))]
    (timbre/debug {:message           "Checking Stinky Pinky guess"
                   :workspace-id      workspace-id
                   :channel-id        channel-id
                   :user-id           user-id
                   :guess             guess
                   :is-guess-correct? is-guess-correct?})
    is-guess-correct?))

(defn reset-game
  "Reset a Stinky Pinky game to a new host."
  [tx token workspace-id channel-id user-id]
  (as-> {:workspace_id workspace-id
         :channel_id   channel-id} $
    (db.stinkypinky/get-stinky-pinky-details tx $)
    (assoc $
           :host user-id
           :solution nil
           :clue nil
           :hint nil)
    (db.stinkypinky/set-stinky-pinky-details tx $)))

(defn check-guess
  "Check if a message is a Stinky Pinky guess and check if it is correct
  for the channel."
  [tx token workspace-id channel-id user-id text]
  (when-let [guess (guess text)]
    (if (is-guess-correct? tx token workspace-id channel-id user-id guess)
      (do
        (db.stinkypinky/mark-winner tx {:workspace_id workspace-id
                                        :channel_id   channel-id
                                        :winner       user-id})
        (reset-game tx token workspace-id channel-id user-id)
        (slack/send-message token {:channel channel-id
                                   :text    (str "<@" user-id "> got it! They will be "
                                                 "hosting a new round. Type `/sp help` "
                                                 "to learn the commands for hosting.")}))
      (do
        (db.stinkypinky/mark-guess tx {:workspace_id workspace-id
                                       :channel_id   channel-id
                                       :guesser      user-id
                                       :guess        guess})
        (slack/send-message token {:channel channel-id
                                   :text    (str "Wrong <@" user-id ">!")})))))

(defn set-answer
  [tx token workspace-id channel-id solution]
  (let [details (db.stinkypinky/get-stinky-pinky-details tx {:workspace_id workspace-id
                                                             :channel_id   channel-id})]
    (->> solution
         (assoc details :solution)
         (db.stinkypinky/set-stinky-pinky-details tx))
    (slack/send-message token
                        {:channel channel-id
                         :text    (str "An answer has been set for Stinky Pinky. Game on!")})))

(defn set-clue
  [tx token workspace-id channel-id clue]
  (let [details (db.stinkypinky/get-stinky-pinky-details tx {:workspace_id workspace-id
                                                             :channel_id   channel-id})]
    (->> clue
         (assoc details :clue)
         (db.stinkypinky/set-stinky-pinky-details tx))
    (slack/send-message token
                        {:channel channel-id
                         :text    (str "The Stinky Pinky clue is: _" clue "_")})))

(defn set-hint
  [tx token workspace-id channel-id hint]
  (let [details (db.stinkypinky/get-stinky-pinky-details tx {:workspace_id workspace-id
                                                             :channel_id   channel-id})]
    (->> hint
         (assoc details :hint)
         (db.stinkypinky/set-stinky-pinky-details tx))
    (slack/send-message token
                        {:channel channel-id
                         :text    (str "The Stinky Pinky hint is: _" hint "_")})))

(defn set-channel
  "Set the channel ID given as a stinky pinky channel."
  [tx token workspace-id channel-id user-id]
  (if (= 1 (db.stinkypinky/add-stinky-pinky-channel tx
                                                    {:workspace_id workspace-id
                                                     :channel_id   channel-id
                                                     :host         user-id}))
    (slack/send-message token
                        {:channel channel-id
                         :text    (str "This channel has been set as a Stinky Pinky channel.")})
    (timbre/error {:message      "Could not set channel as Stinky Pinky channel"
                   :workspace-id workspace-id
                   :channel-id   channel-id})))

(defn show-scores
  "Send the scores of the current game to the channel."
  [tx token workspace-id channel-id user-id]
  (if-let [counts (db.stinkypinky/get-stinky-pinky-scores tx
                                                          {:workspace_id workspace-id
                                                           :channel_id   channel-id
                                                           :user_id      user-id})]
    (let [msg (->> counts
                   (map-indexed (fn [i {:keys [winner_count winner]}]
                                  (str (inc i) ". *" winner "* has _" winner_count "_ points.")))
                   (interpose \newline)
                   (apply str))]
      (slack/send-message token
                          {:channel     channel-id
                           :attachments [{:title    "Stinky Pinky Scores"
                                          :fallback msg
                                          :text     msg}]})
      :sent)
    :no-scores))

(defn wrap-stinky-pinky-guess
  "Hydrate the Stinky Pinky channel details in the request object."
  [handler]
  (fn [{{:keys [team_id event]} :body-params
        tx                      :slackbot.database/tx
        token                   :slackbot.slack/oauth-access-token :as req}]
    (let [{:keys [channel user type text]} event]
      (when (= "message" type)
        (check-guess tx token team_id channel user text)))
    (handler req)))
