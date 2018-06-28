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

(ns slackbot.web-server
  (:require
   [immutant.web :refer [run stop]]
   [mount.core :refer [defstate]]
   [slackbot.config :as config]
   [slackbot.routes :refer [app]]))

(defstate web-server
  :start (let [opts (config/config [:web-server])]
           (run app opts))
  :stop (stop web-server))
