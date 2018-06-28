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

(ns slackbot.database.karma
  (:require
   [hugsql.core :as hugsql]
   [jdbc.core :as jdbc]
   [slackbot.database :as db]))

(hugsql/def-db-fns "slackbot/database/sql/karma.sql")

(defn inc-karma
  "Increment the target's karma by an arbitrary amount."
  [conn args]
  (if (= 1 (increment-karma conn args))
    (target-karma conn args)
    (if (= 1 (set-karma conn args))
      (target-karma conn args)
      (throw
       (ex-info "Could not set karma for target" args)))))

(defn dec-karma
  "Decrement the target's karma by an arbitrary amount."
  [conn args]
  (if (= 1 (decrement-karma conn args))
    (target-karma conn args)
    (if (= 1 (set-karma conn (update args :karma #(* -1 %))))
      (target-karma conn args)
      (throw
       (ex-info "Could not set karma for target" args)))))
