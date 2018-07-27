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

CREATE TABLE stinkypinky (
  workspace_id       TEXT NOT NULL,
  channel_id         TEXT NOT NULL,
  game_id            INTEGER,
  host               TEXT,
  hint               TEXT,
  clue               TEXT,
  solution           TEXT,
  PRIMARY KEY (workspace_id, channel_id)
  FOREIGN KEY (workspace_id) REFERENCES teams(workspace_id)
);
--;;
CREATE UNIQUE INDEX stinkypinky_games
ON stinkypinky (workspace_id, channel_id, game_id);
--;;
CREATE TABLE stinkypinky_guesses (
  workspace_id       TEXT NOT NULL,
  channel_id         TEXT NOT NULL,
  guesser            TEXT NOT NULL,
  guess              TEXT NOT NULL,
  FOREIGN KEY (workspace_id) REFERENCES teams(workspace_id)
);
--;;
CREATE TABLE stinkypinky_winners (
  workspace_id       TEXT NOT NULL,
  channel_id         TEXT NOT NULL,
  winner             TEXT NOT NULL,
  FOREIGN KEY (workspace_id) REFERENCES teams(workspace_id)
);
