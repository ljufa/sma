DEFAULT_TAG ?= latest

build_jar:
	cd .. && ./gradlew build -x test

build_ui:
	cd ../web-ui/ && cargo make build_release

build_docker:
	docker build ../tw-stream-collector -t ljufa/tw-stream-collector:$(DEFAULT_TAG)
	docker build ../tw-data -t ljufa/tw-data:$(DEFAULT_TAG)
	docker build ../backend -t ljufa/backend:$(DEFAULT_TAG)
	docker build ../web-ui -t ljufa/web-ui:$(DEFAULT_TAG)

#redeploy_local: DEFAULT_TAG:=$(shell uuidgen)
redeploy_local: build_jar build_ui build_docker
	TAG=$(DEFAULT_TAG) docker-compose -f docker-compose-server.yaml up -d

logs:
	docker-compose -f docker-compose-server.yaml logs -f --tail 200

stop:
	docker-compose -f docker-compose-server.yaml stop