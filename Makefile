ALL:    docker

TAG = $(shell git rev-parse HEAD | cut -b 1-7)

target/l10n.war:
	mvn install

docker: target/l10n.war
	docker build -t jenkinsci/l10n-server:$(TAG) .
