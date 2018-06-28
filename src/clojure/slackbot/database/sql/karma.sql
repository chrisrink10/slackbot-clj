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

-- :name target-karma :? :1
-- :doc Get the target's karma from the workspace.
SELECT karma FROM karma
WHERE workspace_id = :workspace_id
AND target = :target

-- :name top-karma :? :*
-- :doc Get the top karma from the workspace.
SELECT target, karma FROM karma
WHERE workspace_id = :workspace_id
ORDER BY karma DESC
LIMIT :n

-- :name- increment-karma :! :n
-- :doc Increment the karma of a target.
INSERT OR REPLACE INTO karma
  (workspace_id, target, karma)
SELECT workspace_id, target, karma + :karma
FROM karma
WHERE workspace_id = :workspace_id
AND target = :target

-- :name- decrement-karma :! :n
-- :doc Decrement the karma of a target.
INSERT OR REPLACE INTO karma
  (workspace_id, target, karma)
SELECT workspace_id, target, karma - :karma
FROM karma
WHERE workspace_id = :workspace_id
AND target = :target

-- :name- set-karma :! :n
-- :doc Set the karma of a target.
INSERT OR REPLACE INTO karma
  (workspace_id, target, karma)
VALUES
  (:workspace_id, :target, :karma)
