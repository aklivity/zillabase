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
package io.aklivity.zillabase.cli.internal.commands.stop;

import java.util.List;
import java.util.Map;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.RemoveNetworkCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Network;
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.internal.commands.ZillabaseDockerCommand;

@Command(
    name = "stop",
    description = "Stop containers for local development")
public final class ZillabaseStopCommand extends ZillabaseDockerCommand
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

        List<Container> containers = client.listContainersCmd()
            .withLabelFilter(project)
            .withShowAll(true)
            .exec();

        containers.stream()
            .filter(c -> "running".equals(c.getState()))
            .forEach(c -> stopContainer(client, c.getId()));

        containers
            .forEach(c -> removeContainer(client, c.getId()));

        networks.stream()
            .filter(n -> networkName.equals(n.getName()))
            .forEach(n -> removeNetwork(client, n.getId()));
    }

    private static void stopContainer(
        DockerClient client,
        String id)
    {
        try (StopContainerCmd command = client.stopContainerCmd(id))
        {
            command.exec();
        }
    }

    private static void removeContainer(
        DockerClient client,
        String id)
    {
        try (RemoveContainerCmd command = client.removeContainerCmd(id)
                .withForce(true))
        {
            command.exec();
        }
    }

    private static void removeNetwork(
        DockerClient client,
        String id)
    {
        try (RemoveNetworkCmd command = client.removeNetworkCmd(id))
        {
            command.exec();
        }
    }
}
