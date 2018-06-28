-- Copyright © 2018 Chris Rink
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- :name oauth-state :? :1
-- :doc Get the OAuth state, if it exists.
SELECT state FROM oauth
WHERE state = :state

-- :name start-oauth :! :n
-- :doc Create a new OAuth flow.
INSERT INTO oauth
  (state)
VALUES
  (:state)

-- :name delete-oauth :! :n
-- :doc Delete OAuth state.
DELETE FROM oauth WHERE state = :state

-- :name new-workspace :! :n
-- :doc Create a new workspace/team.
INSERT INTO teams
  (workspace_id, oauth_access_token, app_user_id)
VALUES
  (:workspace_id, :oauth_access_token, :app_user_id)

-- :name workspace-details :? :1
-- :doc Get details about the workspace, if it exists.
SELECT workspace_id, oauth_access_token, app_user_id
FROM teams
WHERE workspace_id = :workspace_id
