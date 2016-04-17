FROM jetty:9.3.8
ADD target/l10n.war /var/lib/jetty/webapps/l10n.war

# directory where the submissions are recorded
# should be mounted from outside
ENV DATADIR /var/l10n/data

CMD java -jar "$JETTY_HOME/start.jar"