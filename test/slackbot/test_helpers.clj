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

(ns slackbot.test-helpers
  (:import
   java.io.File)
  (:require
   [mount.core :as mount]
   [slackbot.database :as db]
   [slackbot.database.migrations :as db.migrations]))

(defn create-temp-file
  [^String filename ^String ext]
  (File/createTempFile filename ext))

(defmacro with-temp-file
  [[temp-name prefix ext] & body]
  `(let [f#         (create-temp-file ~prefix ~ext)
         ~temp-name (.getAbsolutePath f#)]
     (try
       (with-open [file# (clojure.java.io/writer f#)]
         ~@body)
       (finally
         (.delete f#)))))

(defn wrap-refresh-db
  "Fixture to reset and migrate a fresh database."
  [run-test]
  (with-temp-file [filepath "slackbot-test" ".db"]
    (let [jdbc-url (str "jdbc:sqlite:" filepath)]
      (mount/start-with {#'db/datasource-options {:jdbc-url jdbc-url}})
      (db.migrations/migrate jdbc-url)
      (run-test)
      (mount/stop))))
