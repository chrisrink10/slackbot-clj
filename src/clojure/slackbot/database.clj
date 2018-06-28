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
   [hikari-cp.core :as hikari-cp]
   [hugsql.core :as hugsql]
   [hugsql.adapter.clojure-jdbc :as cj-adapter]
   [jdbc.core :as jdbc]
   [mount.core :refer [defstate]]
   [slackbot.config :as config]))

(defstate datasource-options
  :start {:jdbc-url (config/config [:database :connection-string])})

(defstate datasource
  :start (hikari-cp/make-datasource datasource-options)
  :stop  (hikari-cp/close-datasource datasource))

(defstate db-spec
  :start {:datasource datasource})

(defstate conn
  :start (jdbc/connection db-spec)
  :stop  (.close conn))

(defstate hugsql-adapter
  :start (hugsql/set-adapter! (cj-adapter/hugsql-adapter-clojure-jdbc)))
