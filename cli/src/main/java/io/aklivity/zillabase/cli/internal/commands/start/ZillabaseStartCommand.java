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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.github.dockerjava.api.model.RestartPolicy.unlessStoppedRestart;
import static io.aklivity.zillabase.cli.config.ZillabaseConfig.DEFAULT_RISINGWAVE_PORT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.fusesource.jansi.Ansi;

import com.asyncapi.bindings.kafka.v0._5_0.server.KafkaServerBinding;
import com.asyncapi.schemas.asyncapi.Reference;
import com.asyncapi.v2._6_0.model.channel.message.Message;
import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.asyncapi.v3._0_0.model.info.License;
import com.asyncapi.v3._0_0.model.operation.Operation;
import com.asyncapi.v3._0_0.model.operation.OperationAction;
import com.asyncapi.v3._0_0.model.operation.reply.OperationReply;
import com.asyncapi.v3._0_0.model.server.Server;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseDockerCommand;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.add.ZillabaseAsyncapiAddCommand;
import io.aklivity.zillabase.cli.internal.config.ZillabaseConfigAdapter;

@Command(
    name = "start",
    description = "Start containers for local development")
public final class ZillabaseStartCommand extends ZillabaseDockerCommand
{
    private static final int RISINGWAVE_INITIALIZATION_DELAY_MS = 1000;
    private static final int MAX_RETRIES = 5;

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

        ZillabaseConfig config;
        Path configPath = Paths.get("zillabase/config.yaml");

        try (InputStream inputStream = Files.newInputStream(configPath))
        {
            JsonbConfig jsonbConfig = new JsonbConfig().withAdapters(new ZillabaseConfigAdapter());
            Jsonb jsonb = JsonbBuilder.create(jsonbConfig);
            config = jsonb.fromJson(inputStream, ZillabaseConfig.class);
        }
        catch (IOException | JsonbException ex)
        {
            config = new ZillabaseConfig();
        }

        List<String> containerIds = new LinkedList<>();
        for (CreateContainerFactory factory : factories)
        {
            try (CreateContainerCmd command = factory.createContainer(client, config))
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

        Path seedPath = Paths.get("zillabase/seed.sql");
        if (Files.exists(seedPath))
        {
            try
            {
                String content = Files.readString(seedPath);
                Properties props = new Properties();
                props.setProperty("user", "root");

                if (processSeedSql(content, props, config))
                {
                    System.out.println("seed.sql processed successfully!");
                }
                else
                {
                    System.err.println("Failed to process seed.sql after " + MAX_RETRIES + " attempts.");
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.err);
            }
        }

        Path kafkaSpecPath = Paths.get("asyncapi-kafka.yaml");
        String kafkaSpec = null;
        if (Files.exists(kafkaSpecPath))
        {
            try
            {
                kafkaSpec = Files.readString(kafkaSpecPath);
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
        else
        {
            List<KafkaTopicSchemaRecord> records = resolveKafkaTopicsAndSchemas(config);
            if (!records.isEmpty())
            {
                kafkaSpec = generateKafkaAsyncApiSpecs(config, records);
            }
        }

        if (kafkaSpec != null)
        {
            try
            {
                System.out.println(kafkaSpec);
                File tempFile = File.createTempFile("zillabase-kafka-asyncapi-spec", ".tmp");

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
                {
                    writer.write(kafkaSpec);
                }

                ZillabaseAsyncapiAddCommand command = new ZillabaseAsyncapiAddCommand();
                command.artifactId = "zillabase-kafka-asyncapi-%s".formatted(System.currentTimeMillis());
                command.helpOption = new HelpOption<>();
                command.spec = tempFile.getPath();
                command.run();
                tempFile.delete();
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
    }

    private List<KafkaTopicSchemaRecord> resolveKafkaTopicsAndSchemas(
        ZillabaseConfig config)
    {
        List<KafkaTopicSchemaRecord> records = new ArrayList<>();
        try (AdminClient adminClient = AdminClient.create(Map.of(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafkaBootstrapUrl)))
        {
            final HttpClient client = HttpClient.newHttpClient();

            KafkaFuture<Collection<TopicListing>> topics = adminClient.listTopics().listings();
            for (TopicListing topic : topics.get())
            {
                if (!topic.isInternal())
                {
                    String topicName = topic.name();

                    StringBuilder label = new StringBuilder(topicName.length());
                    boolean capitalizeNext = true;
                    for (char c : topicName.toCharArray())
                    {
                        if (c == '-')
                        {
                            capitalizeNext = true;
                        }
                        else
                        {
                            if (capitalizeNext)
                            {
                                c = Character.toUpperCase(c);
                                capitalizeNext = false;
                            }
                            label.append(c);
                        }
                    }

                    String subject = "%s-value".formatted(topicName);
                    String schema = resolveSchema(config, client, subject);
                    if (schema != null)
                    {
                        String type = resolveType(config, client, subject);
                        records.add(new KafkaTopicSchemaRecord(topicName, label.toString(), subject, type, schema));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println("Error resolving Kafka Topics & Schemas Info");
        }
        return records;
    }

    public String generateKafkaAsyncApiSpecs(
        ZillabaseConfig config,
        List<KafkaTopicSchemaRecord> records)
    {
        String spec = null;
        try
        {
            final AsyncAPI asyncAPI = new AsyncAPI();
            final Components components = new Components();
            final Map<String, Object> schemas = new HashMap<>();
            final Map<String, Object> messages = new HashMap<>();
            final Map<String, Object> channels = new HashMap<>();
            final Map<String, Object> operations = new HashMap<>();

            Message message;
            Channel channel;
            Operation operation;
            Reference reference;

            asyncAPI.setAsyncapi("3.0.0");

            Info info = new Info();
            info.setTitle("API Document for Kafka Cluster");
            info.setVersion("1.0.0");
            License license = new License("Aklivity Community License",
                "https://github.com/aklivity/zillabase/blob/develop/LICENSE");
            info.setLicense(license);
            asyncAPI.setInfo(info);

            Server server = new Server();
            server.setHost(config.kafkaBootstrapUrl);
            server.setProtocol("kafka");

            KafkaServerBinding kafkaServerBinding = new KafkaServerBinding();
            kafkaServerBinding.setSchemaRegistryUrl(config.registryUrl);
            kafkaServerBinding.setSchemaRegistryVendor("apicurio");
            server.setBindings(Collections.singletonMap("kafka", kafkaServerBinding));
            asyncAPI.setServers(Collections.singletonMap("plain", server));

            for (KafkaTopicSchemaRecord record : records)
            {
                String name = record.name;
                String label = record.label;
                String subject = record.subject;
                String messageName = "%sMessage".formatted(label);

                channel = new Channel();
                channel.setAddress(name);
                reference = new Reference("#/components/messages/%s".formatted(messageName));
                channel.setMessages(Collections.singletonMap(messageName, reference));
                channels.put(name, channel);

                ObjectMapper schemaMapper = new ObjectMapper();
                schemas.put(subject, schemaMapper.readTree(record.schema));

                message = new Message();
                message.setName(messageName);
                message.setContentType("application/%s".formatted(record.type));

                reference = new Reference("#/components/schemas/%s".formatted(subject));
                message.setPayload(reference);
                messages.put(messageName, message);

                operation = new Operation();
                operation.setAction(OperationAction.SEND);
                reference = new Reference("#/channels/%s".formatted(name));
                operation.setChannel(reference);
                reference = new Reference("#/channels/%s/messages/%s".formatted(name, messageName));
                operation.setMessages(Collections.singletonList(reference));
                if (name.endsWith("-commands"))
                {
                    String replyTopic = name.replace("-commands", "-replies");
                    OperationReply reply = new OperationReply();
                    reference = new Reference("#/channels/%s".formatted(replyTopic));
                    reply.setChannel(reference);
                    operation.setReply(reply);
                }

                operations.put("do%s".formatted(label), operation);

                operation = new Operation();
                operation.setAction(OperationAction.RECEIVE);
                reference = new Reference("#/channels/%s".formatted(name));
                operation.setChannel(reference);
                reference = new Reference("#/channels/%s/messages/%s".formatted(name, messageName));
                operation.setMessages(Collections.singletonList(reference));
                operations.put("on%s".formatted(label), operation);
            }

            components.setSchemas(schemas);
            components.setMessages(messages);

            asyncAPI.setComponents(components);
            asyncAPI.setChannels(channels);
            asyncAPI.setOperations(operations);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.setSerializationInclusion(NON_NULL);

            spec = mapper.writeValueAsString(asyncAPI);
        }
        catch (Exception ex)
        {
            System.err.println("Error generating Kafka AsyncApi Spec");
        }
        return spec;
    }

    private String resolveType(
        ZillabaseConfig config,
        HttpClient client,
        String subject)
    {
        String type = null;
        try
        {
            HttpRequest httpRequest = HttpRequest
                .newBuilder(toURI(config.registryUrl,
                    "/apis/registry/v2/groups/%s/artifacts/%s/meta".formatted(config.registryGroupId, subject)))
                .GET()
                .build();

            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200)
            {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(httpResponse.body());
                type = rootNode.path("type").asText().toLowerCase();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
        return type;
    }

    private String resolveSchema(
        ZillabaseConfig config,
        HttpClient client,
        String subject)
    {
        String responseBody;
        try
        {
            HttpRequest httpRequest = HttpRequest
                .newBuilder(toURI(config.registryUrl,
                    "/apis/registry/v2/groups/%s/artifacts/%s".formatted(config.registryGroupId, subject)))
                .GET()
                .build();

            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            responseBody = httpResponse.statusCode() == 200 ? httpResponse.body() : null;
        }
        catch (Exception ex)
        {
            responseBody = null;
        }
        return responseBody;
    }

    private URI toURI(
        String baseUrl,
        String path)
    {
        return URI.create(baseUrl).resolve(path);
    }

    private boolean processSeedSql(
        String content,
        Properties props,
        ZillabaseConfig config)
    {
        boolean status = false;
        int retries = 0;
        int delay = RISINGWAVE_INITIALIZATION_DELAY_MS;

        while (retries < MAX_RETRIES)
        {
            try
            {
                Thread.sleep(delay);
                try (Connection conn = DriverManager.getConnection("jdbc:postgresql://%s/%s"
                    .formatted(config.risingWaveUrl, config.risingWaveDb), props);
                     Statement stmt = conn.createStatement())
                {
                    String[] sqlCommands = content.split(";");
                    for (String command : sqlCommands)
                    {
                        if (!command.trim().isEmpty())
                        {
                            stmt.execute(command);
                        }
                    }
                    status = true;
                    break;
                }
            }
            catch (InterruptedException | SQLException ex)
            {
                retries++;
                delay *= 2;
            }
        }
        return status;
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
        private static final String ZILLABASE_HOSTNAME_FORMAT = "%s.zillabase.dev";

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
            this.hostname = String.format(ZILLABASE_HOSTNAME_FORMAT, name);
        }

        abstract CreateContainerCmd createContainer(
            DockerClient client,
            ZillabaseConfig config);
    }

    private static final class CreateZillaFactory extends CreateContainerFactory
    {
        CreateZillaFactory()
        {
            super("zilla", "ghcr.io/aklivity/zilla:latest");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client,
            ZillabaseConfig config)
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
            DockerClient client,
            ZillabaseConfig config)
        {
            ExposedPort exposedPort = ExposedPort.tcp(9092);

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(network)
                        .withRestartPolicy(unlessStoppedRestart()))
                        .withPortBindings(new PortBinding(Ports.Binding.bindPort(9092), exposedPort))
                .withExposedPorts(exposedPort)
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
            DockerClient client,
            ZillabaseConfig config)
        {
            ExposedPort exposedPort = ExposedPort.tcp(8080);

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(network)
                        .withRestartPolicy(unlessStoppedRestart()))
                        .withPortBindings(new PortBinding(Ports.Binding.bindPort(8080), exposedPort))
                .withExposedPorts(exposedPort)
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
            DockerClient client,
            ZillabaseConfig config)
        {
            ExposedPort exposedPort = ExposedPort.tcp(DEFAULT_RISINGWAVE_PORT);

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(network)
                        .withPortBindings(new PortBinding(Ports.Binding.bindPort(DEFAULT_RISINGWAVE_PORT), exposedPort))
                        .withRestartPolicy(unlessStoppedRestart()))
                .withExposedPorts(exposedPort)
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
            DockerClient client,
            ZillabaseConfig config)
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
            DockerClient client,
            ZillabaseConfig config)
        {
            List<String> envVars = Arrays.asList(
                "ADMIN_PORT=%d".formatted(config.port),
                "REGISTRY_URL=%s".formatted(config.adminConfig.registryUrl),
                "REGISTRY_GROUP_ID=%s".formatted(config.adminConfig.registryGroupId),
                "DEBUG=%s".formatted(true));

            int port = config.port;
            ExposedPort exposedPort = ExposedPort.tcp(port);
            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network))
                    .withPortBindings(new PortBinding(Ports.Binding.bindPort(port), exposedPort))
                .withExposedPorts(exposedPort)
                .withEnv(envVars)
                .withTty(true);
        }
    }
}
