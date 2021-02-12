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

(ns slackbot.proxy
  (:require
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [httpurr.client.aleph :as http]
   [httpurr.status :as s]
   [medley.core :refer [assoc-some]]
   [mount.core :refer [defstate]]
   [promesa.core :as p]
   [taoensso.timbre :as timbre]
   [slackbot.config :as config]))

(defn my-ip-address
  "Fetch my current IP address from Amazon."
  []
  (-> (http/get "https://checkip.amazonaws.com/")
      (p/then (fn [{:keys [body] :as response}]
                (cond
                  (s/success? response)
                  (str/trim-newline (slurp body))

                  :else
                  (throw
                   (ex-info "Could not find my IP address" {:response response})))))
      (deref)))

(defstate current-ip
  :start (my-ip-address))

(defn proxy
  "Send a proxy request to the specified `proxy-url`."
  [{:keys [body headers query-string request-method uri]} proxy-url]
  (let [url (str proxy-url uri)]
    (->> {:body         body
          :headers      headers
          :method       request-method
          :query-string query-string
          :url          url}
         (http/send!)
         (deref))))

(defn add-forwarded-header
  "Add the `X-Forwarded-For` header to a proxied request."
  [{:strs [x-forwarded-for] :as headers}]
  (cond->> current-ip
    (some? x-forwarded-for) (str x-forwarded-for ", ")
    :always                 (assoc headers "x-forwarded-for")))

(defn wrap-proxy-requests
  "Middleware which proxies requests to a remote server if `request-matches?`
  returns true for the request.

  If the `[:slackbot :proxy :bot-api-url]` configuration option is not supplied,
  then requests will be proxied and `handler` will be returned as by `identity`."
  [handler request-matches?]
  (if-let [proxy-url (config/config [:proxy :bot-api-url])]
    (fn [req]
      (if (request-matches? req)
        (-> req
            (update :headers add-forwarded-header)
            (proxy proxy-url))
        (handler req)))
    handler))

(defn wrap-proxy-split-requests
  "Middleware which proxies requests to a remote server as the _real_ request, but
  has the current host process the response and emit a debug log for inspection.

  If the `[:slackbot :proxy :bot-api-url]` configuration option is not supplied,
  then requests will be proxied and `handler` will be returned as by `identity`."
  [handler]
  (if-let [proxy-url (config/config [:proxy :bot-api-url])]
    (fn [req]
      (do
        ;; Handle the request internally, discarding the results
        (let [internal-response (handler req)]
          (timbre/debug
           (str "INTERNAL RESPONSE:" \newline
                (with-out-str (pprint/pprint internal-response)))))

        ;; Proxy the request to the remote server and return it
        (-> req
            (update :headers add-forwarded-header)
            (proxy proxy-url)))
      (handler req))
    handler))
