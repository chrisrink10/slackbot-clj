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

-- :name add-stinky-pinky-channel :! :n
-- :doc Add a channel as a host channel for Stinky Pinky.
INSERT INTO stinkypinky
  (workspace_id, channel_id, host)
VALUES
  (:workspace_id, :channel_id, :host)

-- :name- remove-stinky-pinky-channel :! :n
-- :doc Remove a channel as a host channel for Stinky Pinky.
DELETE FROM stinkypinky
WHERE workspace_id = :workspace_id
AND channel_id = :channel_id

-- :name get-stinky-pinky-details :? :1
-- :doc Get the current Stinky Pinky details.
SELECT workspace_id, channel_id, game_id, host, hint, clue, solution
FROM stinkypinky
WHERE workspace_id = :workspace_id
AND channel_id = :channel_id

-- :name is-solution-set? :? :1
-- :doc Return 1 if a solution is set for the current channel.
SELECT solution IS NOT NULL as solution_is_set
FROM stinkypinky
WHERE workspace_id = :workspace_id
AND channel_id = :channel_id

-- :name check-stinky-pinky-guess :? :1
-- :doc Get the Stinky Pinky channels for the workspace.
SELECT host = :user_id AS is_guesser_host, 1 AS is_guess_correct
FROM stinkypinky
WHERE workspace_id = :workspace_id
AND channel_id = :channel_id
AND solution = :guess

-- :name set-stinky-pinky-details :! :n
-- :doc Set the details for this Stinky Pink round.
INSERT OR REPLACE INTO stinkypinky
  (workspace_id, channel_id, game_id, host, hint, clue, solution)
VALUES
  (:workspace_id, :channel_id, :game_id, :host, :hint, :clue, :solution)

-- :name get-stinky-pinky-scores :? :n
-- :doc Get the Stinky Pinky scores.
SELECT COUNT(winner) AS winner_count, winner
FROM stinkypinky_winners
WHERE workspace_id = :workspace_id
AND channel_id = :channel_id
GROUP BY winner
LIMIT :n

-- :name get-stinky-pinky-guesses :? :n
-- :doc Get the Stinky Pinky scores.
SELECT guesser, guess
FROM stinkypinky_guesses
WHERE workspace_id = :workspace_id
AND channel_id = :channel_id

-- :name mark-winner :! :n
-- :doc Mark the winner of a Stinky Pinky round.
INSERT INTO stinkypinky_winners
  (workspace_id, channel_id, winner)
VALUES
  (:workspace_id, :channel_id, :winner)

-- :name mark-guess :! :n
-- :doc Mark the guess of a Stinky Pinky round.
INSERT INTO stinkypinky_guesses
  (workspace_id, channel_id, guesser, guess)
VALUES
  (:workspace_id, :channel_id, :guesser, :guess)
