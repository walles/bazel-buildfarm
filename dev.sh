#! /bin/bash
set -ex

bazelisk build //src/main/java/build/buildfarm:buildfarm-shard-worker_deploy.jar

chmod 755 bazel-bin/src/main/java/build/buildfarm/buildfarm-shard-worker_deploy.jar
scp bazel-bin/src/main/java/build/buildfarm/buildfarm-shard-worker_deploy.jar gew1-preprodbuildfarmworker-d-076x.gew1.spotify.net:~/

# on the remote:
# disco role preprodbuildfarmworker to pick one instance
# systemctl stop helios-agent.service
# sudo java -XX:StartFlightRecording=delay=5s,duration=120s,settings=profile,filename=/tmp/$(date +%Y_%m_%d-%H_%M_%S).jfr -jar buildfarm-shard-worker_deploy.jar /etc/spotify/worker.config
# 

ssh -A gew1-preprodbuildfarmworker-d-076x.gew1.spotify.net sudo java -XX:StartFlightRecording=delay=5s,duration=120s,settings=profile,filename=/tmp/$(date +%Y_%m_%d-%H_%M_%S).jfr -jar buildfarm-shard-worker_deploy.jar /etc/spotify/worker.config

