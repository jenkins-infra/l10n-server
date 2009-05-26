#!/bin/bash
mvn clean install
scp target/l10n.war hudson-ci.org:~/l10n-server/
