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

(ns slackbot.routes.slash-command.karma
  (:require
   [clojure.string :as str]
   [ring.util.response :as response]
   [slackbot.karma :as karma]))

(def karma-command-pattern #"(\S+)(?: ?(.*))")

(defmulti handle-karma
  (fn [{:keys [text]} _ _]
    (if-let [[_ command _] (re-matches karma-command-pattern text)]
      command
      "help"))
  :default "help")

(defmethod handle-karma "show"
  [{:keys [team_id channel_id text]} tx token]
  (if-let [[_ target] (re-matches #"show (\S+)" text)]
    (do
      (karma/report-target-karma tx token team_id channel_id target)
      (-> (response/response nil)
          (response/status 204)) )
    (-> {:response_type "ephemeral"
         :text          "Karma targets must be a single word."}
        (response/response))))

(defmethod handle-karma "top"
  [{:keys [team_id channel_id]} tx token]
  (karma/report-top-karma tx token team_id channel_id)
  (-> (response/response nil)
      (response/status 204)))

(defmethod handle-karma "give"
  [{:keys [team_id channel_id text]} tx token]
  (if-let [[_ target n] (re-matches #"give (\S+)( \d+)?" text)]
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
        (response/response))))

(defmethod handle-karma "take"
  [{:keys [team_id channel_id text]} tx token]
  (if-let [[_ target n] (re-matches #"take (\S+)( \d+)?" text)]
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

(defmethod handle-karma "help"
  [_ _ _]
  (-> {:response_type "ephemeral"
       :text          (->> [(str "Karma is a way to keep track of who is great and who is terrible. "
                                 "Type `[target]++` into any channel to give `[target]` a karma point. "
                                 "For example, if you want to give Zelda a karma point for doing something "
                                 "cool, type `zelda++`. If Jerry does something terrible, type `jerry--` to "
                                 "subtract a karma point from Jerry. Targets of karma manipulation do not "
                                 "need to be Slack users. Any string will do, so have some fun!")
                            "Available `/karma` sub-commands are:"
                            "- `give [target n]` - give `target` `n` karma, or 1 if `n` is not specified"
                            "- `take [target n]` - take `n` karma from `target`, or 1 if `n` is not specified"
                            "- `show [target]` - show the karma for `target`"
                            "- `top` - report the top targets by karma"
                            "- `help` - show this text"]
                           (interpose \newline)
                           (apply str))}
      (response/response)))
