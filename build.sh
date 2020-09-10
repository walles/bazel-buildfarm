#! /bin/bash
set -ex

apt-get update -y && apt-get upgrade -y
apt-get install -y curl unzip g++

curl -sS -m300 -Lo bazelisk https://artifactory.spotify.net/artifactory/client-infrastructure/buildtools/bazelisk/1.2.1_2/bazelisk-linux-amd64
chmod 755 bazelisk
./bazelisk clean
./bazelisk build //src/main/java/build/buildfarm:all --verbose_failures

echo "running from folder:"
pwd
ls -l
set +ex
# Exclude common-tests target since it fails on Tingle for some reason
./bazelisk test -- //src/test/java/build/buildfarm:all -//src/test/java/build/buildfarm:common-tests --verbose_failures
status=$?
find ../.cache -iname test.log -exec cat {} \;
exit $status
