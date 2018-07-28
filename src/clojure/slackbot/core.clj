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

(ns slackbot.core
  (:gen-class)
  (:require
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [mount.core :as mount]
   [slackbot.database]
   [slackbot.database.migrations :as migrations]
   [slackbot.logging]
   [slackbot.web-server]))

(defn usage
  [_]
  (->> ["Usage: slackbot action"
        ""
        "Actions:"
        "  start    Start a new server"
        "  migrate  Perform all pending migrations"
        ""]
       (str/join \newline)))

(defn error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn- start
  "Start the Slack Bot server application."
  []
  (mount/start))

(defn -main
  "Entry point to the application."
  [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args [])]
    ;; Handle help and error conditions
    (cond
      (:help options)            (exit 0 (usage summary))
      (not= (count arguments) 1) (exit 1 (usage summary))
      errors                     (exit 1 (error-msg errors)))

    ;; Execute program with options
    (case (first arguments)
      "start"   (start)
      "migrate" (migrations/migrate)
      (exit 1 (usage summary)))))
