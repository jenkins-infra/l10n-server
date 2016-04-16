ALL:    docker

IMAGE = jenkinsciinfra/l10n-server
TAG = $(shell git rev-parse HEAD | cut -b 1-7)

target/l10n.war:
	mvn install

data:
	mkdir data

docker: target/l10n.war
	docker build -t $(IMAGE):$(TAG) .

run: docker data
	docker run -ti --rm -p 8085:8080 -u `id -u` -v `pwd`/data:/var/l10n $(IMAGE):$(TAG)

# hit the endpoint as a test
test:
	curl -H 'Referer: http://test/' http://localhost:8085/l10n/submit\?$$(echo '{"locale":"bogus","submitter":"bogus","version":"bogus", "id":"bogus"}' | gzip -c | base64 -w0)
