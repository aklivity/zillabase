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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseDockerCommand;

@Command(
    name = "start",
    description = "Start containers for local development")
public final class ZillabaseStartCommand extends ZillabaseDockerCommand
{
    @Override
    protected void invoke(
        DockerClient client)
    {
        Map<String, String> project = Map.of("com.docker.compose.project", "zillabase");

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

        List<String> containerIds = new ArrayList<>();

        try (CreateContainerCmd command = client
            .createContainerCmd("ghcr.io/aklivity/zilla:latest")
            .withLabels(project)
            .withName(String.format(format, "zilla"))
            .withCmd("start", "-v")
            .withTty(true)
            .withHostConfig(HostConfig.newHostConfig()
                .withNetworkMode(networkName)))
        {
            CreateContainerResponse zilla = command.exec();

            containerIds.add(zilla.getId());
        }

        for (String containerId : containerIds)
        {
            try (StartContainerCmd command = client.startContainerCmd(containerId))
            {
                command.exec();
            }
        }
    }
}
