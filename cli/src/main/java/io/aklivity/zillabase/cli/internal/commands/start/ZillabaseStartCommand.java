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

import static com.github.dockerjava.api.model.RestartPolicy.unlessStoppedRestart;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;

import org.fusesource.jansi.Ansi;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseDockerCommand;
import io.aklivity.zillabase.cli.internal.config.ZillabaseConfigAdapter;

@Command(
    name = "start",
    description = "Start containers for local development")
public final class ZillabaseStartCommand extends ZillabaseDockerCommand
{
    @Override
    protected void invoke(
        DockerClient client)
    {

        new CreateNetworkFactory().createNetwork(client);

        List<CreateContainerFactory> factories = new LinkedList<>();
        factories.add(new CreateAdminFactory());
        factories.add(new CreateZillaFactory());
        factories.add(new CreateKafkaFactory());
        factories.add(new CreateRisingWaveFactory());
        factories.add(new CreateApicurioFactory());
        factories.add(new CreateKeycloakFactory());

        for (CreateContainerFactory factory : factories)
        {
            String repository = factory.image;
            try (PullImageCmd command = client.pullImageCmd(repository))
            {
                ReentrantLock lock = new ReentrantLock();
                Condition complete = lock.newCondition();

                lock.lock();
                try
                {
                    command.exec(new PullImageProgressHandler(System.out, lock, complete));
                    complete.awaitUninterruptibly();
                }
                finally
                {
                    lock.unlock();
                }
            }
        }

        List<String> containerIds = new LinkedList<>();
        for (CreateContainerFactory factory : factories)
        {
            try (CreateContainerCmd command = factory.createContainer(client))
            {
                CreateContainerResponse response = command.exec();
                containerIds.add(response.getId());
            }
        }

        for (String containerId : containerIds)
        {
            try (StartContainerCmd command = client.startContainerCmd(containerId))
            {
                command.exec();
            }
        }
    }

    private static final class PullImageProgressHandler extends ResultCallback.Adapter<PullResponseItem>
    {
        private final PrintStream out;
        private final Lock lock;
        private final Condition complete;

        private final Map<String, ResponseItem> items;

        private PullImageProgressHandler(
            PrintStream out,
            Lock lock,
            Condition complete)
        {
            this.out = out;
            this.lock = lock;
            this.complete = complete;
            this.items = new LinkedHashMap<>();
        }

        @Override
        public void onNext(
            PullResponseItem item)
        {
            Ansi ansi = Ansi.ansi();
            for (int i = 0; i < items.size(); i++)
            {
                ansi.eraseLine();
                ansi.cursorUpLine();
            }
            out.print(ansi);

            String itemId = item.getId();
            if (itemId != null)
            {
                String from = item.getFrom();
                String layer = String.format("%s:%s", from, itemId);
                items.put(layer, item);
            }

            for (ResponseItem value : items.values())
            {
                String id = value.getId();
                String progress = value.getProgress();
                String status = value.getStatus();

                switch (status)
                {
                case "Downloading":
                    out.format("%s: %s\n", id, progress);
                    break;
                default:
                    out.format("%s: %s\n", id, status);
                    break;
                }
            }
        }

        @Override
        public void onError(
            Throwable throwable)
        {
            doSignalComplete();
        }

        @Override
        public void onComplete()
        {
            doSignalComplete();
        }

        private void doSignalComplete()
        {
            lock.lock();
            try
            {
                complete.signal();
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    private abstract static class CommandFactory
    {
        static final String ZILLABASE_NAME_FORMAT = "zillabase_%s";

        final String network;

        CommandFactory()
        {
            this.network = String.format(ZILLABASE_NAME_FORMAT, "default");
        }
    }

    private static final class CreateNetworkFactory extends CommandFactory
    {
        void createNetwork(
            DockerClient client)
        {
            List<Network> networks = client.listNetworksCmd()
                    .exec();

            if (!networks.stream()
                    .map(Network::getName)
                    .anyMatch(network::equals))
            {
                client.createNetworkCmd()
                    .withName(network)
                    .withDriver("bridge")
                    .exec();
            }
        }
    }

    private abstract static class CreateContainerFactory extends CommandFactory
    {
        final Map<String, String> project;
        final String name;
        final String image;
        final String hostname;

        CreateContainerFactory(
            String name,
            String image)
        {
            this.project = Map.of("com.docker.compose.project", "zillabase");
            this.name = String.format(ZILLABASE_NAME_FORMAT, name);
            this.image = image;
            this.hostname = name;
        }

        abstract CreateContainerCmd createContainer(
            DockerClient client);
    }

    private static final class CreateZillaFactory extends CreateContainerFactory
    {
        CreateZillaFactory()
        {
            super("zilla", "ghcr.io/aklivity/zilla:latest");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(network))
                .withCmd("start", "-v", "-e")
                .withTty(true);
        }
    }

    private static final class CreateKafkaFactory extends CreateContainerFactory
    {
        CreateKafkaFactory()
        {
            super("kafka", "bitnami/kafka:3.2.3");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(network)
                        .withRestartPolicy(unlessStoppedRestart()))
                .withTty(true)
                .withEnv(
                    "ALLOW_PLAINTEXT_LISTENER=yes",
                    "KAFKA_CFG_NODE_ID=1",
                    "KAFKA_CFG_BROKER_ID=1",
                    "KAFKA_CFG_GROUP_INITIAL_REBALANCE_DELAY_MS=0",
                    "KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@127.0.0.1:9093",
                    "KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CLIENT:PLAINTEXT,INTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT",
                    "KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER",
                    "KAFKA_CFG_LOG_DIRS=/tmp/logs",
                    "KAFKA_CFG_PROCESS_ROLES=broker,controller",
                    "KAFKA_CFG_LISTENERS=CLIENT://:9092,INTERNAL://:29092,CONTROLLER://:9093",
                    "KAFKA_CFG_INTER_BROKER_LISTENER_NAME=INTERNAL",
                    "KAFKA_CFG_ADVERTISED_LISTENERS=CLIENT://localhost:9092,INTERNAL://kafka:29092",
                    "KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true");
        }
    }

    private static final class CreateApicurioFactory extends CreateContainerFactory
    {
        CreateApicurioFactory()
        {
            super("apicurio", "apicurio/apicurio-registry-mem:latest-release");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(network)
                        .withRestartPolicy(unlessStoppedRestart()))
                .withTty(true);
        }
    }

    private static final class CreateRisingWaveFactory extends CreateContainerFactory
    {
        CreateRisingWaveFactory()
        {
            super("risingwave", "risingwavelabs/risingwave:latest");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(network)
                        .withRestartPolicy(unlessStoppedRestart()))
                .withTty(true)
                .withCmd("playground");
        }
    }

    private static final class CreateKeycloakFactory extends CreateContainerFactory
    {
        CreateKeycloakFactory()
        {
            super("keycloak", "bitnami/keycloak:latest");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(network)
                        .withRestartPolicy(unlessStoppedRestart()))
                .withTty(true)
                .withEnv(
                    "KEYCLOAK_DATABASE_VENDOR=dev-file");
        }
    }

    private static final class CreateAdminFactory extends CreateContainerFactory
    {
        CreateAdminFactory()
        {
            super("admin", "ghcr.io/aklivity/zillabase/admin:latest");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            ZillabaseConfig config;
            Path configPath = Paths.get("zillabase/config.yaml");

            try (InputStream inputStream = Files.newInputStream(configPath))
            {
                inputStream.reset();
                JsonbConfig jsonbConfig = new JsonbConfig().withAdapters(new ZillabaseConfigAdapter());
                Jsonb jsonb = JsonbBuilder.create(jsonbConfig);

                config = jsonb.fromJson(inputStream, ZillabaseConfig.class);
            }
            catch (IOException | JsonbException ex)
            {
                config = new ZillabaseConfig();
            }

            int port = config.port;
            ExposedPort exposedPort = ExposedPort.tcp(port);
            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network))
                    .withBinds(new Bind(configPath.toAbsolutePath().toString(), new Volume("/app/zillabase/config.yaml")))
                    .withPortBindings(new PortBinding(Ports.Binding.bindPort(port), exposedPort))
                .withExposedPorts(exposedPort)
                .withTty(true);
        }
    }
}
