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
  (:import
   org.sqlite.SQLiteException)
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [ring.util.response :as response]
   [reitit.ring :as ring]
   [taoensso.timbre :as timbre]
   [slackbot.karma :as karma]
   [slackbot.slack :as slack]
   [slackbot.stinkypinky :as stinky-pinky]))

(def stinky-pinky-command-pattern #"(\S+)(?: ?(.*))")

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
  [{{:keys [team_id channel_id text]} :body-params
    tx                                :slackbot.database/tx
    token                             :slackbot.slack/oauth-access-token}]
  (if-not (str/blank? text)
    (->> (str/trim text)
         (karma/report-target-karma tx token team_id channel_id))
    (karma/report-top-karma tx token team_id channel_id)))

(defmethod handle "/give-karma"
  [{{:keys [team_id channel_id text]} :body-params
    tx                                :slackbot.database/tx
    token                             :slackbot.slack/oauth-access-token}]
  (if-let [[_ target n] (re-matches #"(\S+)( \d+)?" text)]
    (let [amount (if (some? n)
                   (Integer/parseInt (str/trim n))
                   1)]
      (cond
        (not (int? amount))
        (-> {:response_type "ephemeral"
             :text          "Karma must be an integer."}
            (response/response))

        (< amount 1)
        (-> {:response_type "ephemeral"
             :text          "Karma must be greater than or equal to 1."}
            (response/response))

        :else
        (do
          (karma/give-karma tx token team_id channel_id target amount)
          (-> (response/response nil)
              (response/status 204)))))
    (-> {:response_type "ephemeral"
         :text          "Specify a target and karma as a positive integer."}
        (response/response)) ))

(defmethod handle "/take-karma"
  [{{:keys [team_id channel_id text]} :body-params
    tx                                :slackbot.database/tx
    token                             :slackbot.slack/oauth-access-token}]
  (if-let [[_ target n] (re-matches #"(\S+)( \d+)?" text)]
    (let [amount (if (some? n)
                   (Integer/parseInt (str/trim n))
                   1)]
      (cond
        (not (int? amount))
        (-> {:response_type "ephemeral"
             :text          "Karma must be an integer."}
            (response/response))

        (< amount 1)
        (-> {:response_type "ephemeral"
             :text          "Karma must me greater than or equal to 1."}
            (response/response))

        :else
        (do
          (karma/take-karma tx token team_id channel_id target amount)
          (-> (response/response nil)
              (response/status 204)))))
    (-> {:response_type "ephemeral"
         :text          "Specify a target and karma as a positive integer."}
        (response/response))))

(defmulti handle-stinky-pinky
  (fn [{:keys [text]} _ _]
    (if-let [[_ command _] (re-matches stinky-pinky-command-pattern text)]
      command
      "help"))
  :default "help")

(defmethod handle-stinky-pinky "set-answer"
  [{:keys [team_id channel_id text]} tx token]
  (if-let [[_ answer] (re-matches #"set-answer (\S+ \S+)" text)]
    (do
      (stinky-pinky/set-answer tx token team_id channel_id answer)
      (-> (response/response nil)
          (response/status 204)))
    (-> {:response_type "ephemeral"
         :text          "Stinky Pinky answers must be two rhyming words."}
        (response/response))))

(defmethod handle-stinky-pinky "set-clue"
  [{:keys [team_id channel_id text]} tx token]
  (if-let [[_ clue] (re-matches #"set-clue (.*)" text)]
    (do
      (stinky-pinky/set-clue tx token team_id channel_id clue)
      (-> (response/response nil)
          (response/status 204)))
    (-> {:response_type "ephemeral"
         :text          (str "Your Stinky Pinky clue didn't quite work. Try `/sp set-clue smelly finger`")}
        (response/response))))

(defmethod handle-stinky-pinky "set-hint"
  [{:keys [team_id channel_id text]} tx token]
  (if-let [[_ hint] (re-matches #"set-hint (\S+ \S+)" text)]
    (do
      (stinky-pinky/set-hint tx token team_id channel_id hint)
      (-> (response/response nil)
          (response/status 204)))
    (-> {:response_type "ephemeral"
         :text          (str "Stinky Pinky hints must be two rhyming words, "
                             "like: `stink pink`, `stinky pinky`, `stinkity pinkity`, etc.")}
        (response/response))))

(defmethod handle-stinky-pinky "set-channel"
  [{:keys [team_id channel_id user_id]} tx token]
  (try
    (do
      (stinky-pinky/set-channel tx token team_id channel_id user_id)
      (-> (response/response nil)
          (response/status 204)))
    (catch SQLiteException _
      (-> {:response_type "ephemeral"
           :text          "This channel is already a Stinky Pinky channel."}
          (response/response)))))

(defmethod handle-stinky-pinky "show-scores"
  [{:keys [team_id channel_id user_id]} tx token]
  (case (stinky-pinky/show-scores tx token team_id channel_id user_id)
    :no-scores   (-> {:response_type "ephemeral"
                      :text          "No Stinky Pinky scores found for this channel."}
                     (response/response))
    :scores-sent (-> (response/response nil)
                     (response/status 204))))

(defmethod handle-stinky-pinky "help"
  [_ _ _]
  (-> {:response_type "ephemeral"
       :text          (->> [(str "Stinky Pinky is a word game. The game is played in rounds, "
                                 "each hosted by a single player. The host of the round will "
                                 "think of a pair of words that rhyme (like \"stinky pinky\"), "
                                 "for instance. The host will also think of a clue (like \"smelly "
                                 " finger\" for the previous answer). Other players enter a clue "
                                 "into the Stinky Pinky channel by guessing word pairs until the "
                                 "answer is guessed. The player which correctly guesses the pair "
                                 "hosts the next round. The game can continue ad nauseum or to a "
                                 "terminal score.")
                            "Available `/sp` sub-commands are:"
                            "- `set-answer [answer]` - if you are the host, set the answer for this round"
                            "- `set-clue [clue]` - if you are the host, set the clue for this round"
                            "- `set-hint [hint]` - if you are the host, set the hint for this round"
                            "- `set-channel` - set the current channel as a Stinky Pinky channel and set you as the host of the first round"
                            "- `show-scores` - show the scores for players in the current channel's Stinky Pinky game"
                            "- `reset` - reset the scores and finish the game"
                            "- `help` - show this text"]
                           (interpose \newline)
                           (apply str))}
      (response/response)))

(defmethod handle "/sp"
  [{body-params :body-params
    tx          :slackbot.database/tx
    token       :slackbot.slack/oauth-access-token}]
  (handle-stinky-pinky body-params tx token))

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
