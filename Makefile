TOP_DIR=.
README=$(TOP_DIR)/README.md

VERSION=$(strip $(shell cat version))

build:
	@echo "Building the software..."

init: install dep
	@echo "Initializing the repo..."

travis-init:
	@echo "Initialize software required for travis (normally ubuntu software)"

install:
	@echo "Install software required for this repo..."

dep:
	@echo "Install dependencies required for this repo..."
	@gradlew buildDependents

pre-build: install dep
	@echo "Running scripts before the build..."

post-build:
	@echo "Running scripts after the build is done..."

all: pre-build build post-build

test:
	@echo "Running test suites..."

lint:
	@echo "Linting the software..."

doc:
	@echo "Building the documenation..."

precommit: dep lint

travis-deploy: release
	@echo "Deploy the software by travis"

clean:
	@echo "Cleaning the build..."

watch:
	@make build
	@echo "Watching templates and slides changes..."
	@fswatch -o src/ | xargs -n1 -I{} make build

run:
	@echo "Running the software..."

maven: maven-legacy maven-cbor

maven-legacy:
	@echo "uploading sdk-protobuf + wallet-sdk to maven"
	@./gradlew clean assemble
	@./gradlew :sdk-protobuf:javadoc
	@./gradlew :sdk-protobuf:sourceSets
	@./gradlew :sdk-protobuf:publishAllPublicationsToMavenRepository
	@./gradlew :wallet-sdk:publish

# canonical-cbor and tx-codec are standalone Gradle projects (their own
# settings.gradle, modern plugins block) because the root build still
# references the shut-down jcenter() repo. They're published with the
# system `gradle` binary (CI installs Gradle 9.x via setup-gradle@v3),
# not the root wrapper (Gradle 7.2). When the root build is modernised
# these can fold back into `maven-legacy`.
#
# Uses the vanniktech maven-publish plugin's
# `publishAndReleaseToMavenCentral` task: uploads the staging deployment
# to the Sonatype Central Portal and (because publishToMavenCentral is
# called with automaticRelease=true in build.gradle) auto-releases it
# once Central's validation passes — no manual "Close → Release" click
# in the Portal UI required.
#
# Requires `~/.gradle/gradle.properties`:
#   mavenCentralUsername=<central-portal-user-token-name>
#   mavenCentralPassword=<central-portal-user-token-secret>
#   signing.gnupg.keyName=<last-8-hex-of-publisher-gpg-long-key-id>
maven-cbor:
	@echo "uploading canonical-cbor + tx-codec to Sonatype Central Portal"
	@cd canonical-cbor && gradle clean publishAndReleaseToMavenCentral --no-configuration-cache
	@cd tx-codec && gradle clean publishAndReleaseToMavenCentral --no-configuration-cache


mavenLocal: mavenLocal-legacy mavenLocal-cbor

mavenLocal-legacy:
	@echo "uploading sdk-protobuf + wallet-sdk to mavenLocal"
	@./gradlew clean assemble
	@./gradlew publishToMavenLocal

mavenLocal-cbor:
	@echo "uploading canonical-cbor + tx-codec to mavenLocal (standalone)"
	@cd canonical-cbor && gradle clean publishToMavenLocal
	@cd tx-codec && gradle clean publishToMavenLocal


include .makefiles/*.mk

.PHONY: build init travis-init install dep pre-build post-build all test doc precommit travis clean watch run bump-version create-pr
