# Server side for translation plugin
This webapp receives submissions from translation plugin and stores them as JSON files locally.

## Building

    mvn install
    docker build -t jenkinsci/l10n-server .