# slackbot-clj

A Slackbot written in Clojure.

## Getting Started

### With Docker

Bring your database up to date, then start the Slackbot and `nginx`
reverse proxy:

```bash
docker-compose up --build migrate
docker-compose up --build -d slackbot
docker-compose up -d nginx
```

### For Development

You can start a REPL with `lein repl` and start the web-server with
`(reset)` once the REPL is started.

## Features

 * Give people or things _karma_ points using simple `++` or `--` 
   suffixes: `zelda++` or `jerry--`

 * Play Stinky Pinky with your friends or colleagues using `/sp`

## License

Copyright © 2018 Chris Rink

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
