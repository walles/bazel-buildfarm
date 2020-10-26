// Copyright 2020 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package build.buildfarm.admin.aws;

import build.buildfarm.admin.Admin;
import build.buildfarm.v1test.GetHostsResult;
import build.buildfarm.v1test.Host;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.InstancesDistribution;
import com.amazonaws.services.autoscaling.model.MixedInstancesPolicy;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.SendCommandRequest;
import com.google.protobuf.util.Timestamps;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AwsAdmin implements Admin {
  private static final Logger logger = Logger.getLogger(Admin.class.getName());
  private final AmazonAutoScaling scale;
  private final AmazonEC2 ec2;
  private final AWSSimpleSystemsManagement ssm;

  public AwsAdmin(String region) {
    scale = AmazonAutoScalingClientBuilder.standard().withRegion(region).build();
    ec2 = AmazonEC2ClientBuilder.standard().withRegion(region).build();
    ssm = AWSSimpleSystemsManagementClientBuilder.standard().withRegion(region).build();
  }

  @Override
  public void terminateHost(String hostId) {
    ec2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(hostId));
    logger.log(Level.INFO, String.format("Terminated host: %s", hostId));
  }

  @Override
  public void stopContainer(String hostId, String containerName) {
    String stopContainerCmd =
        "docker ps | grep " + containerName + " | awk '{print $1 }' | xargs -I {} docker stop {}";
    Map<String, List<String>> parameters = new HashMap<>();
    parameters.put("commands", Collections.singletonList(stopContainerCmd));
    ssm.sendCommand(
        new SendCommandRequest()
            .withDocumentName("AWS-RunShellScript")
            .withInstanceIds(hostId)
            .withParameters(parameters));
    logger.log(
        Level.INFO, String.format("Stopped container: %s on host: %s", containerName, hostId));
  }

  @Override
  public GetHostsResult getHosts(String filter, int ageInMinutes, String status) {
    GetHostsResult.Builder resultBuilder = GetHostsResult.newBuilder();
    List<Host> hosts = new ArrayList<>();
    DescribeInstancesResult instancesResult =
        ec2.describeInstances(
            new DescribeInstancesRequest()
                .withFilters(new Filter().withName("tag-value").withValues(filter)));
    Long hostNum = 1L;
    for (Reservation r : instancesResult.getReservations()) {
      for (Instance e : r.getInstances()) {
        Long uptime = getHostUptimeInMinutes(e.getLaunchTime());
        if (e.getPrivateIpAddress() != null
            && uptime > ageInMinutes
            && status.equalsIgnoreCase(e.getState().getName())) {
          Host.Builder hostBuilder = Host.newBuilder();
          hostBuilder.setHostNum(hostNum++);
          hostBuilder.setDnsName(e.getPrivateDnsName());
          hostBuilder.setHostId(e.getInstanceId());
          hostBuilder.setIpAddress(e.getPrivateIpAddress());
          hostBuilder.setLaunchTime(Timestamps.fromMillis(e.getLaunchTime().getTime()));
          hostBuilder.setLifecycle(
              e.getInstanceLifecycle() != null ? e.getInstanceLifecycle() : "on demand");
          hostBuilder.setNumCores(e.getCpuOptions().getCoreCount());
          hostBuilder.setState(e.getState().getName());
          hostBuilder.setType(e.getInstanceType());
          hostBuilder.setUptimeMinutes(uptime);
          hosts.add(hostBuilder.build());
        }
      }
    }
    resultBuilder.addAllHosts(hosts);
    resultBuilder.setNumHosts(hosts.size());
    logger.log(Level.FINE, String.format("Got %d hosts for filter: %s", hosts.size(), filter));
    return resultBuilder.build();
  }

  @Override
  public void scaleCluster(
      String scaleGroupName,
      Integer minHosts,
      Integer maxHosts,
      Integer targetHosts,
      Integer targetReservedHostsPercent) {
    UpdateAutoScalingGroupRequest request =
        new UpdateAutoScalingGroupRequest().withAutoScalingGroupName(scaleGroupName);
    if (minHosts != null) {
      request.setMinSize(minHosts);
    }
    if (maxHosts != null) {
      request.setMaxSize(maxHosts);
    }
    if (targetHosts != null) {
      request.setMaxSize(targetHosts);
    }
    if (targetReservedHostsPercent != null) {
      request.setMixedInstancesPolicy(
          new MixedInstancesPolicy()
              .withInstancesDistribution(
                  new InstancesDistribution()
                      .withOnDemandPercentageAboveBaseCapacity(targetReservedHostsPercent)));
    }
    scale.updateAutoScalingGroup(request);
    logger.log(Level.INFO, String.format("Scaled: %s", scaleGroupName));
  }

  private long getHostUptimeInMinutes(Date launchTime) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    return (cal.getTime().getTime() - launchTime.getTime()) / 60000;
  }
}
