# slackbot-clj

A Slackbot written in Clojure with SQLite for persistence.

## Features

 * Give people or things _karma_ points using simple `++` or `--`
   suffixes: `zelda++` or `jerry--`

 * Play Stinky Pinky with your friends or colleagues using `/sp`

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

## Slack Installation

To deploy this application to Slack for use in your workspace, you'll
need to complete the following steps.

### Host the Bot

Your Slackbot instance will need to be hosted and accessible on the public
internet. Additionally, you will need to set up SSL for your domain because
Slack refuses to interact with a Slackbot over insecure connections.

### Create a new Slack App in a Slack Workspace

Slack bots (called Apps in Slack parlance) are always associated with a
home workspace. If the app is not distributed, then that will be the only
workspace it can be used from. Otherwise, it can be installed on any
workspace using an OAuth flow. This Slackbot does support the required
OAuth workflows, but as noted above, it uses SQLite for persistence so it
may be unable to support traffic from a large number of workspaces
simultaneously.

Once you create your Slack app, Slack will create a series of secrets and
tokens which you must supply to this Slackbot via environment variables.

The following environment variables will be required to start the Slackbot,
which should correspond directly with the fields available in Slack's
"Basic Information" and "OAuth & Permissions" panels:

```
export SLACKBOT__WEB_SERVER__HOST='"0.0.0.0"'
export SLACKBOT__SLACK__CLIENT_ID='"..."'
export SLACKBOT__SLACK__CLIENT_SECRET=...
export SLACKBOT__SLACK__VERIFICATION_TOKEN=...
export SLACKBOT__SLACK__REDIRECT_URL=...
```

Note that you may need to wrap certain values in double quotes which are then
wrapped in single quotes in order to ensure that the Slackbot interprets them
as _strings_ rather than numeric values. Certain Slack secrets consist of only
numeric characters, but are not meant to be interpreted as numeric values.

As of this writing, this Slackbot does not support Slack's
[signing secret](https://api.slack.com/docs/verifying-requests-from-slack),
though work to support it is planned.

If you are installing this application, you _should_ enable Slack's "Translate
Global IDs" feature.

### Installing Slash Commands

Slack does not allow workspace apps to surface their slash command support
programmatically, so you must manually install all supported slash commands
in your app.

All slash commands sent from Slack are handled by the same API endpoint:
`/api/slash-command`. This Slackbot currently supports 3 slash commands:

 * `/cooltext [text]` to have the App respond with `[text]` formatted in
   a very cool way
 * `/karma [args]` controls the Karma system (try `/karma help` for more)
 * `/sp [args]` controls the Stinky Pinky game (try `/sp help`)

Slash commands must be installed at the exact named Slash command or this
Slackbot will not respond to them.

### OAuth Scopes

This app may use the following OAuth scopes:

```
channels:history
channels:read
channels:write
chat:write
groups:history
groups:read
groups:write
im:history
im:read
im:write
mpim:history
mpim:read
mpim:write
emoji:read
commands
```

It seems likely it does not require all of these scopes, but I haven't had time
to whittle down the required scopes.

### Events

Events sent by Slack are all handled by the same API endpoint: `/api/slack-event`.
This Slackbot is capable of responding to the following event types:

 * `message.app_home`
 * `message.channels`
 * `message.groups`
 * `message.im`
 * `message.mpim`

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
