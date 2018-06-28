FROM clojure:lein-2.8.1-alpine
MAINTAINER Chris Rink <chrisrink10@gmail.com>

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY project.clj /usr/src/app/
RUN lein deps

COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" slackbot.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "slackbot.jar"]
CMD ["start"]