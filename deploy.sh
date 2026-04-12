#!/bin/bash

# Exit on error
set -e

# Download Heroku
echo "Downloading Heroku JVM Application Deployer..."
curl --location https://github.com/heroku/heroku-jvm-application-deployer/releases/download/v4.0.12/heroku-jvm-application-deployer-4.0.12.jar --output heroku-jvm-application-deployer.jar

# Download New Relic agent
echo "Downloading New Relic agent..."
curl --location https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.19.0/newrelic-java.zip --output newrelic.zip
unzip -o newrelic.zip
rm newrelic.zip

# Ensure the required files exist
if [ ! -f newrelic/newrelic.jar ]; then
  echo "newrelic/newrelic.jar is missing; set up New Relic and copy New Relic JAR files into the newrelic directory"
  exit 1
fi
if [ ! -f target/server.jar ]; then
  echo "target/server.jar is missing or is a directory; target directory contents:"
  ls -la target
  exit 1
fi

# Deploy
echo "Deploying..."
java -jar heroku-jvm-application-deployer.jar \
  --disable-auto-includes \
  --include newrelic \
  --include Procfile \
  --include system.properties \
  target/server.jar
