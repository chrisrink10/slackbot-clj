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

(ns slackbot.routes
  (:require
   [expound.alpha :as expound]
   [mount.core :refer [defstate]]
   [reitit.core :as reitit]
   [reitit.ring :as ring]
   [reitit.ring.spec :as rrs]
   [reitit.spec :as rs]
   [slackbot.routes.app-auth :as app-auth]
   [slackbot.routes.slack-event :as slack-event]
   [slackbot.routes.slash-command :as slash-command]
   [slackbot.middleware :refer [wrap-debug-log-request
                                wrap-format
                                wrap-supply-tx
                                wrap-supply-slack-details
                                wrap-verify-slack-token]]))

(defstate router
  :start (ring/router
          [["/api" {:middleware [wrap-format
                                 wrap-debug-log-request
                                 wrap-verify-slack-token
                                 wrap-supply-tx
                                 wrap-supply-slack-details]}
            ["/slack-event" {:name ::slack-event
                             :post (fn [req]
                                     (slack-event/handle req))}]
            ["/slash-command" {:name ::slash-command
                               :post (fn [req]
                                       (slash-command/handle req))}]]
           ["/app" {:middleware [wrap-format
                                 wrap-debug-log-request
                                 wrap-supply-tx]}
            ["/authorize" {:name ::app-auth
                           :get  app-auth/authorize}]
            ["/install" {:name ::app-install
                         :get  app-auth/install}]]]
          {::rs/explain expound/expound-str
           :validate    rrs/validate-spec!}))

(defstate app
  :start (ring/ring-handler
          router
          (ring/create-default-handler)))
