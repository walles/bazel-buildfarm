#! /bin/bash
set -euxo pipefail

bazel clean
bazel test -- //src/test/java/build/buildfarm:all -//src/test/java/build/buildfarm:common-tests
bazel build //src/main/java/build/buildfarm:buildfarm-server_deploy.jar
bazel build //src/main/java/build/buildfarm:buildfarm-shard-worker_deploy.jar
