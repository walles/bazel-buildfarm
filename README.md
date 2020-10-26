[![Build status](https://badge.buildkite.com/45f4fd4c0cfb95f7705156a4119641c6d5d6c310452d6e65a4.svg?branch=master)](https://buildkite.com/bazel/buildfarm-postsubmit)

# Bazel Buildfarm (fork)

## Words of caution

Before going any further, this repository is a fork of the upstream
[bazel-buildfarm](https://github.com/bazelbuild/bazel-buildfarm) repository.
Ideally all changes should be made in that upstream repository and not in this
fork.

We have two protected branches:
- master: changes get used directly by the buildfarm-server project
- preprod: changes are for the testing environment

The intention is that we should *always* pull changes from upstream first
into the *preprod* branch, and then merge them later to the *master* branch
when ready.

## How does our (Client Build) setup work

This repository is a fork of [bazel-buildfarm](https://github.com/bazelbuild/bazel-buildfarm),
changes ideally should be merged into upstream instead of this repository. Use this
for changes that can't make into upstream for whatever reason, but the goal should be
to merge it in upstream.

This repo reasons of being:

* We need changes specific due to our environment
* Having it in GHE is better than having a fork in the public GitHub since it allows all the team to properly own it.
* We can have CI and deploys for our changes.

### Figuring out which version to use

Check latest commits in this repository and go over to
[buildfarm artifacts in Artifactory](https://artifactory.spotify.net/ui/repos/tree/General/client-infrastructure%2Fbazel%2Fbuildfarm)
to find the matching artifact.

### Contributing

Contribute to this repo as you would to any other:
1. Clone the repo
1. Have your changes on a branch
1. Create a Pull Request
1. Merge if it passes CI and you get an approving review
1. The newest version of `buildfarm` will end up in Artifactory tagged with the SHA of the PR merge commit
    * It will be available in [client-infrastructure/bazel/buildfarm](https://artifactory.spotify.net/ui/repos/tree/General/client-infrastructure%2Fbazel%2Fbuildfarm)

### Synchronizing with upstream

This repository needs to be manually synchronized with upstream. To do so:

1. Clone this repo -- `git clone git@ghe.spotify.net:build/bazel-buildfarm.git`
1. Add upstream as a remote -- `git remote add upstream https://github.com/bazelbuild/bazel-buildfarm.git`
1. Pull from upstream and add fork changes on top -- `git pull -r upstream master`
1. Fix conflicts if needed
1. Push the resulting master to our fork -- `git push -f origin master`

Note that pushing with `-f/--force` is needed since we are rewriting the git history
by applying our fork changes on top of upstream, and we can't really create Pull Requests
out of upstream changes either. This means this operation is risky and is another reason
why all changes should live in upstream.

# The original README:

This repository hosts the [Bazel](https://bazel.build) remote caching and execution system.

Background information on the status of caching and remote execution in bazel can be
found in the [bazel documentation](https://docs.bazel.build/versions/master/remote-caching.html).

File issues here for bugs or feature requests, and ask questions via build team [slack](https://join.slack.com/t/buildteamworld/shared_invite/zt-4zy8f5j5-KwiJuBoAAUorB_mdQHwF7Q) in the #buildfarm channel.

[Buildfarm Wiki](https://github.com/bazelbuild/bazel-buildfarm/wiki)

## Usage

All commandline options override corresponding config settings.

### Bazel Buildfarm Server

Run via

```
bazel run //src/main/java/build/buildfarm:buildfarm-server <configfile> [<-p|--port> PORT]
```

- **`configfile`** has to be in Protocol Buffer text format, corresponding to a [BuildFarmServerConfig](https://github.com/bazelbuild/bazel-buildfarm/blob/master/src/main/protobuf/build/buildfarm/v1test/buildfarm.proto#L55) definition.

  For an example, see the [examples](examples) directory, which contains the working example [examples/server.config.example](examples/server.config.example).
  For format details see [here](https://stackoverflow.com/questions/18873924/what-does-the-protobuf-text-format-look-like). Protocol Buffer structure at [src/main/protobuf/build/buildfarm/v1test/buildfarm.proto](src/main/protobuf/build/buildfarm/v1test/buildfarm.proto)

- **`PORT`** to expose service endpoints on

### Bazel Buildfarm Worker

Run via

```
bazel run //src/main/java/build/buildfarm:buildfarm-operationqueue-worker <configfile> [--root ROOT] [--cas_cache_directory CAS_CACHE_DIRECTORY]
```

- **`configfile`** has to be in Protocol Buffer text format, corresponding to a [WorkerConfig](https://github.com/bazelbuild/bazel-buildfarm/blob/master/src/main/protobuf/build/buildfarm/v1test/buildfarm.proto#L459) definition.

  For an example, see the [examples](examples) directory, which contains the working example [examples/worker.config.example](examples/worker.config.example).
  For format details see [here](https://stackoverflow.com/questions/18873924/what-does-the-protobuf-text-format-look-like). Protocol Buffer structure at [src/main/protobuf/build/buildfarm/v1test/buildfarm.proto](src/main/protobuf/build/buildfarm/v1test/buildfarm.proto)

- **`ROOT`** base directory path for all work being performed.

- **`CAS_CACHE_DIRECTORY`** is (absolute or relative) directory path to cached files from CAS.

### Bazel Client

To use the example configured buildfarm with bazel (version 1.0 or higher), you can configure your `.bazelrc` as follows:

```
$ cat .bazelrc
build --remote_executor=grpc://localhost:8980
```

Then run your build as you would normally do.

### Debugging

Buildfarm uses [Java's Logging framework](https://docs.oracle.com/javase/10/core/java-logging-overview.htm) and outputs all routine behavior to the NICE [Level](https://docs.oracle.com/javase/8/docs/api/java/util/logging/Level.html).

You can use typical Java logging configuration to filter these results and observe the flow of executions through your running services.
An example `logging.properties` file has been provided at [examples/debug.logging.properties](examples/debug.logging.properties) for use as follows:

```
bazel run //src/main/java/build/buildfarm:buildfarm-server -- --jvm_flag=-Djava.util.logging.config.file=$PWD/examples/debug.logging.properties $PWD/examples/server.config.example
```

and

```
bazel run //src/main/java/build/buildfarm:buildfarm-operationqueue-worker -- --jvm_flag=-Djava.util.logging.config.file=$PWD/examples/debug.logging.properties $PWD/examples/worker.config.example
```

To attach a remote debugger, run the executable with the `--debug=<PORT>` flag. For example:

```
bazel run //src/main/java/build/buildfarm:buildfarm-server -- --debug=5005 $PWD/examples/server.config.example
```

## Developer Information

### Building

See build.sh
./bazelisk build //src/main/java/build/buildfarm:all

If you only want one jar, use:
bazelisk build //src/main/java/build/buildfarm:buildfarm-shard-worker_deploy.jar
The jar file will be found under the symlinked folders, for example:
bazel-bin/src/main/java/build/buildfarm/buildfarm-shard-worker_deploy.jar

### Setting up intelliJ

1. Follow the instructions in https://github.com/bazelbuild/intellij to install the bazel plugin for intelliJ
1. Import the project using `ij.bazelproject`

### Third-party Dependencies

Most third-party dependencies (e.g. protobuf, gRPC, ...) are managed automatically via
[rules_jvm_external](https://github.com/bazelbuild/rules_jvm_external). These dependencies are enumerated in
the WORKSPACE with a `maven_install` `artifacts` parameter.

Things that aren't supported by `rules_jvm_external` are being imported as manually managed remote repos via
the `WORKSPACE` file.

### Deployments

Buildfarm can be used as an external repository for composition into a deployment of your choice.

Add the following to your WORKSPACE to get access to buildfarm targets, filling in the commit and sha256 values:

```starlark
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

BUILDFARM_EXTERNAL_COMMIT = "<revision commit id>"
BUILDFARM_EXTERNAL_SHA256 = "<sha256 digest of url below>"

http_archive(
    name = "build_buildfarm",
    strip_prefix = "bazel-buildfarm-%s" % BUILDFARM_EXTERNAL_COMMIT,
    sha256 = BUILDFARM_EXTERNAL_SHA256,
    url = "https://github.com/bazelbuild/bazel-buildfarm/archive/%s.zip" % BUILDFARM_EXTERNAL_COMMIT,
)

load("@build_buildfarm//:deps.bzl", "buildfarm_dependencies")

buildfarm_dependencies()

load("@build_buildfarm//:defs.bzl", "buildfarm_init")

buildfarm_init()
```

Optionally, if you want to use the buildfarm docker container image targets, you can add this:

```starlark
load("@build_buildfarm//:images.bzl", "buildfarm_images")

buildfarm_images()
```
