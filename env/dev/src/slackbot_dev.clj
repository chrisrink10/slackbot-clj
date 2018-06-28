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

(ns slackbot-dev
  (:require
   [clojure.test :as test]
   [clojure.tools.namespace.repl :as repl]
   [mount.core :as mount :refer [defstate]]
   [slackbot.database]
   [slackbot.database.migrations :as migrations]
   [slackbot.web-server]))

(defstate database-migrations
  :start (migrations/migrate))

(defn start
  []
  (mount/start)
  :ready)

(defn stop
  []
  (mount/stop))

(defn reset
  []
  (stop)
  (repl/refresh :after 'slackbot-dev/start))

(defn run-all-tests
  []
  (repl/refresh)
  (test/run-all-tests #"slackbot.*-test"))
