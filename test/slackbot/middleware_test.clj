;; Copyright 2019 Chris Rink
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

(ns slackbot.middleware-test
  (:require
   [clojure.test :refer [deftest is use-fixtures testing]]
   [ring.util.response :as response]
   [slackbot.database.teams :as db.teams]
   [slackbot.middleware :as mw]
   [slackbot.test-helpers :refer [wrap-refresh-db with-temp-db]]))

(use-fixtures :once wrap-refresh-db)

(deftest wrap-query-params-test
  (let [ret     (-> (response/response "True return.")
                    (response/header "content-type" "text/plain")
                    (response/status 201))
        handler (mw/wrap-query-params
                 (fn [{:keys [query-params]}] query-params))]
    (testing "parsing query params"
      (is (= {:a "3" :b "a string"}
             (handler {:query-string "a=3&b=a%20string"})))
      (is (= {:a "3" :b "a string"}
             (handler {:query-string "a=3&b=a+string"})))
      (is (= {:a ["3" "NaN"] :b "a string"}
             (handler {:query-string "a=3&a=NaN&b=a+string"}))))

    (testing "ignoring if no query params"
      (is (nil? (handler {}))))))

(deftest wrap-supply-tx-test
  (let [ret     (-> (response/response "True return.")
                    (response/header "content-type" "text/plain")
                    (response/status 201))
        handler (mw/wrap-supply-tx identity)]
    (is (contains? (handler {}) :slackbot.database/tx))))

(deftest wrap-supply-slack-details
  (with-temp-db *db*
    (let [{:keys [workspace_id oauth_access_token app_user_id] :as db-req}
          {:workspace_id       "FDKHBBBDP832NDD"
           :oauth_access_token "xoxb-BLDKHLDHFENDKJDF"
           :app_user_id        "NNDKHP32352NDB"}

          other-workspace-id "NNBEJDKH3872ND"]
      (db.teams/new-workspace *db* db-req)

      (testing "request from unregistered Slack team ID"
        (let [handler (-> identity
                          (mw/wrap-supply-tx *db*)
                          (mw/wrap-supply-slack-details))]
          (is (= (-> {:message "Invalid team" :team-id other-workspace-id}
                     (response/bad-request))
                 (handler {:body-params {:team_id other-workspace-id
                                         :type    "event"}})))))

      (testing "URL verification request"
        (let [resp    (response/response {})
              handler (-> (constantly resp)
                          (mw/wrap-supply-tx *db*)
                          (mw/wrap-supply-slack-details))]
          (is (= resp (handler {:body-params {:team_id other-workspace-id
                                              :type    "url_verification"}}))))))))

(deftest wrap-verify-slack-token-test
  (let [bad-req-ret (-> {:message "Not Found"}
                        (response/bad-request))
        ret         (-> (response/response "True return.")
                        (response/header "content-type" "text/plain")
                        (response/status 201))
        token       "NENDOXPQWHNPT383N39U8N1ZDEP"
        handler     (mw/wrap-verify-slack-token
                     (constantly ret)
                     token)]
    (testing "valid Slack token"
      (is (= ret (handler {:body-params {:token token}}))))

    (testing "invalid Slack token"
      (is (= bad-req-ret (handler {:body-params {:token ""}})))
      (is (= bad-req-ret (handler {:body-params {:token "not-the-real-token"}})))
      (is (= bad-req-ret (handler {:body-params {:type  "url_verification"
                                                 :event {:user "some-user-id"}}}))))))

(deftest wrap-ignore-myself-test
  (let [user-id    "T89783288222"
        ignore-ret (-> (response/response nil)
                       (response/status 200))
        ret        (-> (response/response "True return.")
                       (response/header "content-type" "text/plain")
                       (response/status 201))
        handler    (mw/wrap-ignore-myself
                    (constantly ret))]
    (testing "message from non-app user"
      (is (= ret (handler {:slackbot.slack/app-user-id user-id
                           :body-params                {:type  "event_callback"
                                                        :event {:user "U8933773733"}}}))))

    (testing "url_verification request"
      (is (= ret (handler {:slackbot.slack/app-user-id user-id
                           :body-params                {:type  "url_verification"
                                                        :event {:user user-id}}}))))

    (testing "ignore myself"
      (is (= ignore-ret
             (handler {:slackbot.slack/app-user-id user-id
                       :body-params            {:type "event_callback"
                                                :event {:user user-id}}}))))))
