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

(ns slackbot.http
  (:require
   [clojure.string :as str]
   [httpurr.client.aleph :as http]
   [muuntaja.core :as m]
   [promesa.core :as p]
   [slackbot.middleware :refer [muuntaja-cache]]))

(defn content-type
  [headers]
  (-> (get headers "content-type")
      (str/split #";")
      (first)
      (str/trim)))

(defn encode
  [{:keys [headers] :as req}]
  (let [content-type (content-type headers)]
    (update req :body #(m/encode (muuntaja-cache) content-type %))))

(defn decode
  [{:keys [headers] :as resp}]
  (let [content-type (content-type headers)]
    (update resp :body #(m/decode (muuntaja-cache) content-type %))))

(defn post
  [uri req]
  (as-> (encode req) $
    (http/post uri $)
    (p/then $ decode)))
