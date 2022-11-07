DEFAULT_TAG ?= latest

build_docker_local:
	../sma-tw-data/gradlew -p ../sma-tw-data build
	docker build ../sma-tw-data -t ljufa/sma-tw-data:$(DEFAULT_TAG)

	../sma-tw-stream-collector/gradlew -p ../sma-tw-stream-collector build
	docker build ../sma-tw-stream-collector -t ljufa/sma-tw-stream-collector:$(DEFAULT_TAG)

	../sma-control/gradlew -p ../sma-control build
	docker build ../sma-control -t ljufa/sma-control:$(DEFAULT_TAG)

	cd ../sma-web-ui/ &&	cargo make build_release &&	docker build . -t ljufa/sma-web-ui:$(DEFAULT_TAG)

redeploy_local: DEFAULT_TAG:=$(shell uuidgen)

redeploy_local: build_docker_local
	TAG=$(DEFAULT_TAG) docker-compose up -d

redeploy_latest:
	TAG=latest docker-compose up -d

redeploy_from_docker_hub:
	docker-compose pull
	TAG=$(DEFAULT_TAG) docker-compose up -d

log:
	docker-compose logs -f --tail 200

SVC =
SVC_TAG =
redeploy_svc: SVC_TAG:=$(shell uuidgen)
redeploy_svc:
	../../$(SVC)/gradlew -p ../../$(SVC) build
	docker build ../../$(SVC) -t ljufa/$(SVC):$(SVC_TAG)
	TAG=$(SVC_TAG) docker-compose up -d $(SVC)
