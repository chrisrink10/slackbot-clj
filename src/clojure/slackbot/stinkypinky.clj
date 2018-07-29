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
  (:import
   org.sqlite.SQLiteException)
  (:require
   [clojure.string :as str]
   [taoensso.timbre :as timbre]
   [slackbot.database.stinkypinky :as db.stinkypinky]
   [slackbot.slack :as slack]
   [slackbot.database :as db]))

(defn normalize-solution
  "Normalize a solution so we don't have to check as many cases in
  the database when a guess comes in."
  [s]
  (-> (str/lower-case s)
      (str/split #" ")
      (sort)
      (->> (str/join " "))))

(defn guess
  "Get a Stinky Pinky guess if it is valid."
  [text]
  (some->> text (re-matches #"(\S+ \S+)") (second) (normalize-solution)))

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
  [tx token user-id text {:keys [solution host] workspace-id :workspace_id channel-id :channel_id}]
  (when-let [guess (guess text)]
    (let [user-ref (slack/user-ref user-id)]
      (cond
        (= host user-id)
        (timbre/info {:message      "Received (possible) guess from Stinky Pinky host"
                      :workspace-id workspace-id
                      :channel-id   channel-id
                      :user-id      user-id
                      :guess        text})

        (nil? solution)
        (do
          (timbre/info {:message      "Received Stinky Pinky guess with no solution set"
                        :workspace-id workspace-id
                        :channel-id   channel-id
                        :user-id      user-id
                        :guess        text})
          (slack/send-ephemeral token
                                {:channel channel-id
                                 :user    user-id
                                 :text    "No solution is set at the moment!"}))

        (= solution guess)
        (do
          (timbre/info {:message      "Received correct Stinky Pinky guess"
                        :workspace-id workspace-id
                        :channel-id   channel-id
                        :user-id      user-id
                        :guess        text})
          (db.stinkypinky/mark-winner tx {:workspace_id workspace-id
                                          :channel_id   channel-id
                                          :winner       user-id})
          (reset-game tx token workspace-id channel-id user-id)
          (slack/send-message token {:channel channel-id
                                     :text    (str user-ref " got it! They will be "
                                                   "hosting a new round. Type `/sp help` "
                                                   "to learn the commands for hosting.")}))

        :else
        (do
          (timbre/info {:message      "Received wrong Stinky Pinky guess"
                        :workspace-id workspace-id
                        :channel-id   channel-id
                        :user-id      user-id
                        :guess        text})
          (db.stinkypinky/mark-guess tx {:workspace_id workspace-id
                                         :channel_id   channel-id
                                         :guesser      user-id
                                         :guess        guess})
          (slack/send-message token {:channel channel-id
                                     :text    (str "Wrong " user-ref "!")}))))))

(defn wrap-stinky-pinky-guess
  "Check Stinky Pinky guesses from incoming messages."
  [handler]
  (fn [{{:keys [team_id event]} :body-params
        tx                      :slackbot.database/tx
        token                   :slackbot.slack/oauth-access-token :as req}]
    (let [{:keys [channel user type text]} event]
      (when (= "message" type)
        (when-let [details
                   (db.stinkypinky/get-stinky-pinky-details tx {:workspace_id team_id
                                                                :channel_id   channel})]
          (check-guess tx token user text details))))
    (handler req)))

(defn- wrap-check-stinky-pinky-host
  "Wrap a function to set new Stinky Pinky details with a check to make
  sure they are the user hosting the current round."
  [tx token workspace-id channel-id user-id f]
  (let [{:keys [host] :as details}
        (db.stinkypinky/get-stinky-pinky-details tx {:workspace_id workspace-id
                                                     :channel_id   channel-id})]
    (if (= host user-id)
      (f details)
      (do
        (timbre/info {:message "User who is not hosting Stinky Pinky attempted to modify SP details"
                      :workspace-id workspace-id
                      :channel-id   channel-id
                      :user-id      user-id})
        (slack/send-ephemeral token
                              {:channel channel-id
                               :user    user-id
                               :text    "You are not hosting this round, so you may not change the Stinky Pinky solution, clue, or hint!"})))))

(defn set-answer
  [tx token workspace-id channel-id user-id solution]
  (wrap-check-stinky-pinky-host
   tx
   token
   workspace-id
   channel-id
   user-id
   (fn [details]
     (->> (normalize-solution solution)
          (assoc details :solution)
          (db.stinkypinky/set-stinky-pinky-details tx))
     (slack/send-message token
                         {:channel channel-id
                          :text    (str "An answer has been set for Stinky Pinky. Game on!")}))))

(defn set-clue
  [tx token workspace-id channel-id user-id clue]
  (wrap-check-stinky-pinky-host
   tx
   token
   workspace-id
   channel-id
   user-id
   (fn [details]
     (->> clue
          (assoc details :clue)
          (db.stinkypinky/set-stinky-pinky-details tx))
     (slack/send-message token
                         {:channel channel-id
                          :text    (str "The Stinky Pinky clue is: _" clue "_")}))))

(defn set-hint
  [tx token workspace-id channel-id user-id hint]
  (wrap-check-stinky-pinky-host
   tx
   token
   workspace-id
   channel-id
   user-id
   (fn [details]
     (->> hint
          (assoc details :hint)
          (db.stinkypinky/set-stinky-pinky-details tx))
     (slack/send-message token
                         {:channel channel-id
                          :text    (str "The Stinky Pinky hint is: _" hint "_")}))))

(defn set-channel
  "Set the channel ID given as a stinky pinky channel."
  [tx token workspace-id channel-id user-id]
  (let [result (try
                 (do
                   (db.stinkypinky/add-stinky-pinky-channel tx
                                                            {:workspace_id workspace-id
                                                             :channel_id   channel-id
                                                             :host         user-id})
                   :successfully-set-channel)
                 (catch SQLiteException e :channel-already-set))]
    (if (= :successfully-set-channel result)
      (slack/send-message token
                          {:channel channel-id
                           :text    (str "This channel has been set as a Stinky Pinky channel.")})
      (timbre/warn {:message      "Could not set channel as Stinky Pinky channel"
                    :workspace-id workspace-id
                    :channel-id   channel-id}))
    result))

(defn show-details
  "Send the details from the current game to the channel."
  [tx token workspace-id channel-id]
  (if-let [{:keys [host hint clue]}
           (db.stinkypinky/get-stinky-pinky-details tx
                                                    {:workspace_id workspace-id
                                                     :channel_id   channel-id})]
    (let [user-ref (slack/user-ref host)
          msg      (str user-ref " is hosting the current round." \newline
                        (if (some? clue)
                          (str "The clue is: `" clue "`.")
                          (str "No clue has been set."))
                        (when (some? hint)
                          (str \newline "The hint is: `" hint "`.")))]
      (slack/send-message token
                          {:channel channel-id
                           :text    msg})
      :details-sent)
    :no-details))

(defn show-guesses
  "Send the guesses from the current game to the channel."
  [tx token workspace-id channel-id]
  (if-let [guesses (db.stinkypinky/get-stinky-pinky-guesses tx
                                                            {:workspace_id workspace-id
                                                             :channel_id   channel-id})]
    (let [msg (->> guesses
                   (map-indexed (fn [i {:keys [guesser guess]}]
                                  (let [user-ref (slack/user-ref guesser)]
                                    (str (inc i) ". *" user-ref "* guessed `" guess "`."))))
                   (interpose \newline)
                   (apply str))]
      (slack/send-message token
                          {:channel     channel-id
                           :attachments [{:title    "Stinky Pinky Guesses"
                                          :fallback msg
                                          :text     msg}]})
      :guesses-sent)
    :no-guesses))

(defn show-scores
  "Send the scores of the current game to the channel."
  [tx token workspace-id channel-id user-id]
  (if-let [counts (db.stinkypinky/get-stinky-pinky-scores tx
                                                          {:workspace_id workspace-id
                                                           :channel_id   channel-id
                                                           :n            20})]
    (let [msg (->> counts
                   (map-indexed (fn [i {:keys [winner_count winner]}]
                                  (let [final-word (cond-> "point" (not= winner_count 1) (str "s"))
                                        user-ref   (slack/user-ref winner)]
                                    (str (inc i) ". *" user-ref "* has _" winner_count "_ " final-word "."))))
                   (interpose \newline)
                   (apply str))]
      (slack/send-message token
                          {:channel     channel-id
                           :attachments [{:title    "Stinky Pinky Scores"
                                          :fallback msg
                                          :text     msg}]})
      :scores-sent)
    :no-scores))
