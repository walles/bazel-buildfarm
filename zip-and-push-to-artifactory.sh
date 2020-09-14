#!/bin/bash
set -euxo pipefail

SHA=$(git rev-parse --verify HEAD)
echo "Current HEAD: ${SHA}"

# Zip whole directory to push to Artifactory
echo "Zipping current dir contents"
ZIP_FILE=bazel-buildfarm-"${SHA}".zip
git archive --format=zip HEAD -o "${ZIP_FILE}"

# Check if SHA is already present in artifactory for whatever reason
echo "Checking if ${ZIP_FILE} is already in Artifactory"
RESPONSE_CODE=$(curl --fail -m300 --head --write-out '%{http_code}' --silent --output /dev/null "https://artifactory.spotify.net/artifactory/cerbero-tests/from-gabriel/${ZIP_FILE}")

if [ "$RESPONSE_CODE" = 200 ]; then
  echo "Skipping since artifact is already published"
else
  echo "Publishing ${ZIP_FILE} to Artifactory"
  curl --fail -m300 -X PUT "https://artifactory.spotify.net/artifactory/cerbero-tests/from-gabriel/${ZIP_FILE}" -T "${ZIP_FILE}"
fi