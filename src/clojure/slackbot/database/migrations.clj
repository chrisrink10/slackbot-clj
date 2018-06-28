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

(ns slackbot.database.migrations
  (:require
   [ragtime.jdbc]
   [ragtime.repl]
   [slackbot.config :as config]))

(defn jdbc-config
  []
  (let [jdbc-url (config/config [:database :connection-string])]
    {:datastore  (ragtime.jdbc/sql-database {:connection-uri jdbc-url})
     :migrations (ragtime.jdbc/load-resources "migrations")}))

(defn migrate
  []
  (ragtime.repl/migrate (jdbc-config)))

(defn rollback
  []
  (ragtime.repl/rollback (jdbc-config)))
