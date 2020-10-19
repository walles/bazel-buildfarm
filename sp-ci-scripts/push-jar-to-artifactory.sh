#!/bin/bash
set -euxo pipefail

JAR_FILE_NAME="${1}"

ARTIFACT_NAME="${2}"

ARTIFACTORY_PATH="client-infrastructure/buildagent/buildfarm/$ARTIFACT_NAME"

if [ "$BRANCH_NAME" = "preprod" ] || [ "$BASE_BRANCH_NAME" = "preprod" ]; then
  ARTIFACTORY_PATH="$ARTIFACTORY_PATH/preprod";
else
  ARTIFACTORY_PATH="$ARTIFACTORY_PATH/prod";
fi

VERSION=$(TZ=UTC date '+%Y%m%d%H%M%S')
WORKER_JAR="bazel-bin/src/main/java/build/buildfarm/$JAR_FILE_NAME"
DEST_URL="https://artifactory.spotify.net/artifactory/$ARTIFACTORY_PATH/$VERSION/$JAR_FILE_NAME-$VERSION.jar"
curl --fail -m300 -XPUT "$DEST_URL" -T "$WORKER_JAR"