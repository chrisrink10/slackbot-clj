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

(ns slackbot.middleware
  (:import
   java.io.InputStream
   java.io.OutputStreamWriter
   java.io.OutputStream)
  (:require
   [clojure.pprint :as pprint]
   [clojure.walk :as walk]
   [next.jdbc :as jdbc]
   [muuntaja.core :as muuntaja]
   [muuntaja.format.core :as fmt]
   [muuntaja.middleware]
   [ring.util.codec :as codec]
   [ring.util.response :as response]
   [taoensso.timbre :as timbre]
   [slackbot.config :as config]
   [slackbot.database :as db]
   [slackbot.database.teams :as db.teams]))

(defn handle-format-exceptions
  "Handle Muuntaja formatting exceptions and return a response."
  [^Exception e fmt _]
  (timbre/error {:message "Could not format request"
                 :format  fmt
                 :error   (ex-message e)})
  {:status  400
   :headers {:content-type "application/json"}
   :body    {:error  "Error occurred attempting to format body to Content-Type"
             :format fmt}})

(defn url-encoder
  [_]
  (reify
    fmt/EncodeToBytes
    (encode-to-bytes [_ data charset]
      (.getBytes (codec/form-encode data ^String charset) ^String charset))

    fmt/EncodeToOutputStream
    (encode-to-output-stream [_ data charset]
      (fn [^OutputStream stream]
        (.write (OutputStreamWriter. stream ^String charset)
                ^String (codec/form-encode data charset))))))

(defn url-decoder
  [_]
  (reify
    fmt/Decode
    (decode [_ data charset]
      (-> (slurp ^InputStream data :encoding charset)
          (codec/form-decode ^String charset)
          (walk/keywordize-keys)))))

(defn muuntaja
  []
  (->> {:name    "application/x-www-form-urlencoded"
        :encoder [url-encoder]
        :decoder [url-decoder]}
       (fmt/map->Format)
       (assoc-in muuntaja/default-options
                 [:formats "application/x-www-form-urlencoded"])
       (muuntaja/create)))

(def muuntaja-cache
  (memoize muuntaja))

(defn wrap-query-params
  "Format a query string into a standard Clojure map."
  [handler]
  (fn [{:keys [query-string] :as req}]
    (handler
     (cond-> req
       (some? query-string)
       (assoc :query-params
              (-> query-string
                  (codec/form-decode)
                  (walk/keywordize-keys)))))))

(defn wrap-format
  "Perform content negotiation based on HTTP header values using Muuntaja."
  [handler]
  (let [m (muuntaja-cache)]
    (-> handler
        (muuntaja.middleware/wrap-params)
        (wrap-query-params)
        (muuntaja.middleware/wrap-format-request m)
        (muuntaja.middleware/wrap-exception handle-format-exceptions)
        (muuntaja.middleware/wrap-format-response m)
        (muuntaja.middleware/wrap-format-negotiate m))))

(defn wrap-supply-tx
  "Hydrate each request with a database transaction."
  [handler]
  (fn [req]
    (jdbc/with-transaction [tx db/datasource]
      (->> tx
           (assoc req :slackbot.database/tx)
           (handler)))))

(defn wrap-supply-slack-details
  "Pull the Slack Team ID from the request and fetch relevant team details
  for this request."
  [handler]
  (fn [{{:keys [team_id type]} :body-params tx :slackbot.database/tx :as req}]
    (if-let [{:keys [oauth_access_token app_user_id]}
             (db.teams/workspace-details tx {:workspace_id team_id})]
      (-> req
          (assoc :slackbot.slack/oauth-access-token oauth_access_token)
          (assoc :slackbot.slack/app-user-id app_user_id)
          (handler))
      (if (= "url_verification" type)
        (do
          (timbre/info {:message "Received URL verification request, passing through without team or app user ID"
                        :type    type})
          (handler req))
        (do
          (timbre/warn {:message "Received request from unregistered team"
                        :team-id team_id})
          (-> {:message "Invalid team" :team-id team_id}
              (response/bad-request)))))))

(defn wrap-verify-slack-token
  "Check for a Slack Verification token in the request body and reject the
  request if it does not match the known verification token."
  [handler]
  (let [verification-token (config/config [:slack :verification-token])]
    (fn [{{:keys [token]} :body-params :as req}]
      (if (not= token verification-token)
        (do
          (timbre/info {:message "Received request with invalid Slack verification token"
                        :token   token})
          (-> {:message "Not Found"}
              (response/bad-request)))
        (handler req)))))

(defn wrap-ignore-myself
  "Ignore Slack Events from the app."
  [handler]
  (fn [{{{:keys [user]} :event type :type} :body-params
        app-user-id                        :slackbot.slack/app-user-id :as req}]
    (if (or (not= app-user-id user) (= "url_verification" type))
      (handler req)
      (do
        (timbre/debug {:message "Ignoring message from myself"})
        (-> (response/response nil)
            (response/status 200))))))

(defn wrap-debug-log-request
  "Emit the entire request map out as a debug log."
  [handler]
  (fn debug-log-request [req]
    (timbre/debug (with-out-str (pprint/pprint req)))
    (let [resp (handler req)]
      (timbre/debug (with-out-str (pprint/pprint resp)))
      resp)))
