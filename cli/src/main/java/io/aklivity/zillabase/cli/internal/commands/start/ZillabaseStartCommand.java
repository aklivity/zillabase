/*
 * Copyright 2024 Aklivity Inc
 *
 * Licensed under the Aklivity Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 *   https://www.aklivity.io/aklivity-community-license/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.aklivity.zillabase.cli.internal.commands.start;

import java.time.Duration;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseCommand;

@Command(
    name = "start",
    description = "Start containers for local development")
public final class ZillabaseStartCommand extends ZillabaseCommand
{
    @Override
    protected void invoke()
    {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("unix:///var/run/docker.sock")
            .withDockerConfig("zillabase")
            .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .maxConnections(100)
            .connectionTimeout(Duration.ofSeconds(30))
            .responseTimeout(Duration.ofSeconds(45))
            .build();

        DockerClient client = DockerClientImpl.getInstance(config, httpClient);

        String format = "zillabase_%s";
        String networkName = String.format(format, "default");

        List<Network> networks = client.listNetworksCmd()
            .exec();

        if (!networks.stream()
                .map(Network::getName)
                .anyMatch(networkName::equals))
        {
            client.createNetworkCmd()
                .withName(networkName)
                .withDriver("bridge")
                .exec();
        }

        CreateContainerResponse zilla = client
            .createContainerCmd("ghcr.io/aklivity/zilla:latest")
            .withName("zillabase_zilla")
            .withCmd("start", "-v")
            .withTty(true)
            .withHostConfig(HostConfig.newHostConfig()
                .withNetworkMode(networkName))
            .exec();

        client.startContainerCmd(zilla.getId())
            .exec();
    }
}
