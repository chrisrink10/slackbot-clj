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

(ns slackbot.database
  (:require
   [hugsql.core :as hugsql]
   [hugsql.adapter.next-jdbc :as next-adapter]
   [next.jdbc :as jdbc]
   [next.jdbc.connection :as jdbc.connection]
   [mount.core :refer [defstate]]
   [slackbot.config :as config])
  (:import
   (com.zaxxer.hikari HikariDataSource)))

(defstate db-spec
  :start {:jdbcUrl (config/config [:database :connection-string])})

(defstate ^HikariDataSource datasource
  :start (jdbc.connection/->pool HikariDataSource db-spec)
  :stop  (.close datasource))

(defstate hugsql-adapter
  :start (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc)))
