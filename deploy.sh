#!/bin/bash
mvn clean install
scp target/l10n.war wsinterop.sun.com:~/l10n-server/
