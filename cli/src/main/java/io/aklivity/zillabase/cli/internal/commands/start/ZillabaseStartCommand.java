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

import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.GET;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.POST;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.PUT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.github.dockerjava.api.model.RestartPolicy.unlessStoppedRestart;
import static io.aklivity.zillabase.cli.config.ZillabaseAdminConfig.DEFAULT_ADMIN_HTTP_PORT;
import static io.aklivity.zillabase.cli.config.ZillabaseAdminConfig.ZILLABASE_ADMIN_SERVER_ZILLA_YAML;
import static io.aklivity.zillabase.cli.config.ZillabaseAuthConfig.DEFAULT_AUTH_HOST;
import static io.aklivity.zillabase.cli.config.ZillabaseAuthConfig.DEFAULT_AUTH_PORT;
import static io.aklivity.zillabase.cli.config.ZillabaseConfigServerConfig.ZILLABASE_CONFIG_KAFKA_TOPIC;
import static io.aklivity.zillabase.cli.config.ZillabaseConfigServerConfig.ZILLABASE_CONFIG_SERVER_ZILLA_YAML;
import static io.aklivity.zillabase.cli.config.ZillabaseKafkaConfig.DEFAULT_KAFKA_BOOTSTRAP_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseKarapaceConfig.DEFAULT_CLIENT_KARAPACE_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseKarapaceConfig.DEFAULT_KARAPACE_URL;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.stream.JsonParsingException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.fusesource.jansi.Ansi;
import org.postgresql.jdbc.PreferQueryMode;

import com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationBinding;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelBinding;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelTopicCleanupPolicy;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelTopicConfiguration;
import com.asyncapi.bindings.kafka.v0._4_0.server.KafkaServerBinding;
import com.asyncapi.schemas.asyncapi.Reference;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuth2SecurityScheme;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuthFlows;
import com.asyncapi.v2._6_0.model.channel.message.Message;
import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.channel.Parameter;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.asyncapi.v3._0_0.model.info.License;
import com.asyncapi.v3._0_0.model.operation.Operation;
import com.asyncapi.v3._0_0.model.operation.OperationAction;
import com.asyncapi.v3._0_0.model.operation.reply.OperationReply;
import com.asyncapi.v3._0_0.model.server.Server;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HealthCheck;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.dockerjava.api.model.Volume;
import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakClientConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakUserConfig;
import io.aklivity.zillabase.cli.config.ZillabaseRisingWaveConfig;
import io.aklivity.zillabase.cli.internal.asyncapi.AsyncapiKafkaFilter;
import io.aklivity.zillabase.cli.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.cli.internal.asyncapi.ZillaHttpOperationBinding;
import io.aklivity.zillabase.cli.internal.asyncapi.ZillaSseKafkaOperationBinding;
import io.aklivity.zillabase.cli.internal.asyncapi.ZillaSseOperationBinding;
import io.aklivity.zillabase.cli.internal.asyncapi.zilla.ZillaAsyncApiConfig;
import io.aklivity.zillabase.cli.internal.asyncapi.zilla.ZillaBindingConfig;
import io.aklivity.zillabase.cli.internal.asyncapi.zilla.ZillaBindingOptionsConfig;
import io.aklivity.zillabase.cli.internal.asyncapi.zilla.ZillaBindingRouteConfig;
import io.aklivity.zillabase.cli.internal.asyncapi.zilla.ZillaCatalogConfig;
import io.aklivity.zillabase.cli.internal.asyncapi.zilla.ZillaGuardConfig;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseDockerCommand;
import io.aklivity.zillabase.cli.internal.commands.asyncapi.add.ZillabaseAsyncapiAddCommand;
import io.aklivity.zillabase.cli.internal.kafka.KafkaBootstrapRecords;
import io.aklivity.zillabase.cli.internal.kafka.KafkaTopicRecord;
import io.aklivity.zillabase.cli.internal.kafka.KafkaTopicSchema;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationsHelper;

@Command(
    name = "start",
    description = "Start containers for local development")
public final class ZillabaseStartCommand extends ZillabaseDockerCommand
{
    private static final int SERVICE_INITIALIZATION_DELAY_MS = 5000;
    private static final int MAX_RETRIES = 5;
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(^|-|_)(.)");
    private static final String DEFAULT_KEYCLOAK_ADMIN_CREDENTIAL = "admin";
    private static final String ADMIN_REALMS_PATH = "/admin/realms";
    private static final String ADMIN_REALMS_CLIENTS_PATH = "/admin/realms/%s/clients";
    private static final String ADMIN_REALMS_CLIENTS_SCOPE_PATH = "/admin/realms/%s/clients/%s/default-client-scopes/%s";
    private static final String ADMIN_REALMS_SCOPE_PATH = "/admin/realms/%s/client-scopes";
    private static final String ADMIN_REALMS_USERS_PATH = "/admin/realms/%s/users";
    private static final Pattern EXPRESSION_PATTERN =
        Pattern.compile("\\$\\{\\{\\s*([^\\s\\}]*)\\.([^\\s\\}]*)\\s*\\}\\}");
    private static final Pattern PROTO_MESSAGE_PATTERN = Pattern.compile("message\\s+\\w+\\s*\\{[^}]*\\}",
        Pattern.DOTALL);
    private static final String KAFKA_ASYNCAPI_ARTIFACT_ID = "kafka-asyncapi";
    private static final String HTTP_ASYNCAPI_ARTIFACT_ID = "http-asyncapi";
    private static final String ZILLABASE_KAFKA_VOLUME = "zillabase_kafka";

    private final Matcher matcher = TOPIC_PATTERN.matcher("");
    private final Matcher envMatcher = EXPRESSION_PATTERN.matcher("");
    private final Matcher protoMatcher = PROTO_MESSAGE_PATTERN.matcher("");
    private final List<String> operations = new ArrayList<>();
    private final List<KafkaTopicSchemaRecord> records = new ArrayList<>();

    private final Path seedSqlPath = ZILLABASE_PATH.resolve("seed.sql");

    public String kafkaSeedFilePath = "zillabase/seed-kafka.yaml";

    @Override
    protected void invoke(
        DockerClient client,
        ZillabaseConfig config)
    {
        startContainers(client, config);

        seedKafkaAndRegistry(config);

        processSql(config);

        createConfigServerKafkaTopic(config);

        processAsyncApiSpecs(config);

        initializeKeycloakService(config);

        publishZillaConfig(config);
    }

    private void startContainers(
        DockerClient client,
        ZillabaseConfig config)
    {
        new CreateNetworkFactory().createNetwork(client);

        List<CreateContainerFactory> factories = new LinkedList<>();
        factories.add(new CreateAuthFactory(config));
        factories.add(new CreateConfigFactory(config));
        factories.add(new CreateZillaFactory(config));
        factories.add(new CreateKafkaFactory(config));
        factories.add(new CreateRisingWaveFactory(config));
        factories.add(new CreateApicurioFactory(config));
        factories.add(new CreateKeycloakFactory(config));
        factories.add(new CreateKarapaceFactory(config));
        factories.add(new CreateAdminFactory(config));
        factories.add(new CreateUdfServerJavaFactory(config));
        factories.add(new CreateUdfServerPythonFactory(config));

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
                String id = response.getId();

                containerIds.add(id);
            }
        }

        for (String containerId : containerIds)
        {
            try (StartContainerCmd command = client.startContainerCmd(containerId))
            {
                command.exec();
            }
        }

        System.out.println("Started containers successfully");

        while (!containerIds.isEmpty())
        {
            for (Iterator<String> i = containerIds.iterator(); i.hasNext(); )
            {
                String containerId = i.next();

                try (InspectContainerCmd command = client.inspectContainerCmd(containerId))
                {
                    InspectContainerResponse response = command.exec();

                    ContainerState state = response.getState();
                    HealthState health = state.getHealth();

                    if (health == null || "healthy".equals(health.getStatus()))
                    {
                        i.remove();
                    }
                }

                Thread.onSpinWait();
            }
        }

        System.out.println("Verified containers are healthy");
    }

    private void processSql(
        ZillabaseConfig config)
    {
        PgsqlHelper pgsql = new PgsqlHelper(config.risingwave);

        pgsql.connect();

        if (pgsql.connected)
        {
            ZillabaseMigrationsHelper migrations = new ZillabaseMigrationsHelper();

            migrations.list()
                .filter(m -> pgsql.connected)
                .forEach(pgsql::process);

            if (pgsql.connected)
            {
                pgsql.process(seedSqlPath);
            }
        }
    }

    private void initializeKeycloakService(
        ZillabaseConfig config)
    {
        String realm = config.keycloak.realm;
        if (realm != null)
        {
            boolean status = false;
            int retries = 0;
            int delay = SERVICE_INITIALIZATION_DELAY_MS;
            String token = null;
            HttpClient client = HttpClient.newHttpClient();
            String url = config.keycloak.url;

            while (retries < MAX_RETRIES)
            {
                try
                {
                    Thread.sleep(delay);
                    String form = "client_id=admin-cli&username=%s&password=%s&grant_type=password"
                        .formatted(DEFAULT_KEYCLOAK_ADMIN_CREDENTIAL, DEFAULT_KEYCLOAK_ADMIN_CREDENTIAL);

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/realms/master/protocol/openid-connect/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    String responseBody = response.body();
                    if (responseBody != null)
                    {
                        JsonReader reader = Json.createReader(new StringReader(responseBody));
                        JsonObject object = reader.readObject();

                        if (object.containsKey("access_token"))
                        {
                            String realmRequestBody = """
                               {
                                "realm": "%s",
                                "enabled": true
                               }
                                """.formatted(realm);

                            token = object.getString("access_token");
                            request = HttpRequest.newBuilder()
                                .uri(URI.create(url + ADMIN_REALMS_PATH))
                                .header("Authorization", "Bearer %s".formatted(token))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(realmRequestBody))
                                .build();

                            response = client.send(request, HttpResponse.BodyHandlers.ofString());

                            if (response.statusCode() == 201)
                            {
                                status = true;
                                break;
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    retries++;
                    delay *= 2;
                }
            }

            if (status)
            {
                System.out.println("Realm: %s created successfully.".formatted(realm));
                List<ZillabaseKeycloakUserConfig> users = config.keycloak.users;
                if (users != null && !users.isEmpty())
                {
                    for (ZillabaseKeycloakUserConfig user : users)
                    {
                        createKeycloakUser(config, client, url, token, user);
                    }
                }
                createKeycloakClientScope(config, client, url, token, realm);
                createKeycloakClient(config, client, url, token);
            }
            else
            {
                System.out.println("Failed to initialize Keycloak Service");
            }
        }
    }

    private void createKeycloakUser(
        ZillabaseConfig config,
        HttpClient client,
        String url,
        String token,
        ZillabaseKeycloakUserConfig user)
    {
        try
        {
            String realm = config.keycloak.realm;
            String[] nameParts = user.name.split(" ");

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode idpNode = mapper.createObjectNode();
            idpNode.put("username", user.username);
            idpNode.put("email", user.email);
            idpNode.put("firstName", nameParts[0]);
            idpNode.put("lastName", nameParts.length > 1 ? nameParts[nameParts.length - 1] : nameParts[0]);
            idpNode.put("enabled", true);

            ObjectNode credentialsNode = mapper.createObjectNode();
            credentialsNode.put("type", "password");
            credentialsNode.put("value", user.password);
            credentialsNode.put("temporary", false);

            ArrayNode credentialsArray = mapper.createArrayNode();
            credentialsArray.add(credentialsNode);

            idpNode.set("credentials", credentialsArray);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(toURI(url, ADMIN_REALMS_USERS_PATH.formatted(realm)))
                .header("Authorization", "Bearer %s".formatted(token))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(idpNode)))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201)
            {
                System.out.println("User: %s created successfully.".formatted(user.name));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    private void createKeycloakClient(
        ZillabaseConfig config,
        HttpClient client,
        String url,
        String token)
    {
        try
        {
            String realm = config.keycloak.realm;
            ZillabaseKeycloakClientConfig keycloakClient = config.keycloak.client;

            if (keycloakClient.secret == null)
            {
                keycloakClient.publicClient = true;
            }
            else
            {
                if (envMatcher.reset(keycloakClient.secret).matches() && "env".equals(envMatcher.group(1)))
                {
                    keycloakClient.secret = System.getenv(envMatcher.group(2));
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(toURI(url, ADMIN_REALMS_CLIENTS_PATH.formatted(realm)))
                .header("Authorization", "Bearer %s".formatted(token))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(keycloakClient)))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201)
            {
                String clientId = keycloakClient.clientId;
                System.out.println("Client: %s created successfully.".formatted(clientId));

                if (config.keycloak.scopes != null && !config.keycloak.scopes.isEmpty())
                {
                    linkScopeWithClient(config, client, url, token);
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    private void linkScopeWithClient(
        ZillabaseConfig config,
        HttpClient client,
        String url,
        String token)
    {
        String realm = config.keycloak.realm;
        String clientId = config.keycloak.client.clientId;
        HttpResponse<String> response;
        HttpRequest request;
        try
        {
            request = HttpRequest.newBuilder()
                .uri(toURI(url, ADMIN_REALMS_CLIENTS_PATH.formatted(realm)))
                .header("Authorization", "Bearer %s".formatted(token))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.body() != null)
            {
                JsonReader reader = Json.createReader(new StringReader(response.body()));
                JsonArray clients = reader.readArray();

                for (JsonValue clientObject: clients)
                {
                    JsonObject keyCloakClient = clientObject.asJsonObject();
                    if (clientId.equals(keyCloakClient.getString("clientId")))
                    {
                        request = HttpRequest.newBuilder()
                            .uri(toURI(url, ADMIN_REALMS_SCOPE_PATH.formatted(realm)))
                            .header("Authorization", "Bearer " + token)
                            .build();

                        response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        if (response.body() != null)
                        {
                            reader = Json.createReader(new StringReader(response.body()));
                            JsonArray scopes = reader.readArray();
                            for (JsonValue scope : scopes)
                            {
                                JsonObject scopeObject = scope.asJsonObject();
                                if (config.keycloak.scopes.contains(scopeObject.getString("name")))
                                {
                                    request = HttpRequest.newBuilder()
                                        .uri(toURI(url, ADMIN_REALMS_CLIENTS_SCOPE_PATH
                                            .formatted(realm, keyCloakClient.getString("id"), scopeObject.getString("id"))))
                                        .header("Authorization", "Bearer %s".formatted(token))
                                        .header("Content-Type", "application/json")
                                        .PUT(HttpRequest.BodyPublishers.noBody())
                                        .build();
                                    client.send(request, HttpResponse.BodyHandlers.ofString());
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    private void createKeycloakClientScope(
        ZillabaseConfig config,
        HttpClient client,
        String url,
        String token,
        String realm)
    {
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode idpNode = mapper.createObjectNode();
            idpNode.put("protocol", "openid-connect");
            idpNode.putPOJO("attributes", Map.of("include.in.token.scope", "true"));

            List<String> scopes = config.keycloak.scopes;
            if (scopes != null && !scopes.isEmpty())
            {
                for (String scope : scopes)
                {
                    idpNode.put("name", scope);

                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(toURI(url, ADMIN_REALMS_SCOPE_PATH.formatted(realm)))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(idpNode)))
                        .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 201)
                    {
                        System.out.println("Scope: %s created successfully".formatted(scope));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    private KafkaBootstrapRecords readKafkaBootstrapRecords()
    {
        KafkaBootstrapRecords records = null;
        Path kafkaSeedPath = Paths.get(kafkaSeedFilePath);
        try
        {
            if (Files.exists(kafkaSeedPath) && Files.size(kafkaSeedPath) != 0 && Files.readAllLines(kafkaSeedPath)
                .stream().anyMatch(line -> !line.trim().isEmpty() && !line.trim().startsWith("#")))
            {
                String content = Files.readString(kafkaSeedPath);
                Jsonb jsonb = JsonbBuilder.create();
                records = jsonb.fromJson(content, KafkaBootstrapRecords.class);
            }
        }
        catch (IOException | JsonParsingException ex)
        {
            System.err.println("Failed to process seed-kafka.yaml : %s".formatted(ex.getMessage()));
        }
        return records;
    }

    private void publishZillaConfig(
        ZillabaseConfig config)
    {
        String zillaConfig = null;

        try
        {
            Path zillaConfigPath = Paths.get("zillabase/zilla/zilla.yaml");
            if (Files.exists(zillaConfigPath))
            {
                zillaConfig = Files.readString(zillaConfigPath);
            }
            else if (!operations.isEmpty())
            {
                List<String> suffixes = Arrays.asList("ReadItem", "Update", "Read", "Create", "Delete",
                    "Get", "GetItem");

                ZillaAsyncApiConfig zilla = new ZillaAsyncApiConfig();
                ZillaCatalogConfig apicurioCatalog = new ZillaCatalogConfig();
                apicurioCatalog.type = "apicurio";
                apicurioCatalog.options = Map.of(
                    "url", config.registry.apicurio.url,
                    "group-id", config.registry.apicurio.groupId);

                ZillaCatalogConfig karapaceCatalog = new ZillaCatalogConfig();
                karapaceCatalog.type = "karapace";
                karapaceCatalog.options = Map.of("url", config.registry.karapace.url);

                ZillabaseKeycloakConfig keycloak = config.keycloak;
                String realm = keycloak.realm;
                String authnJwt = "jwt0";
                if (realm != null)
                {
                    ZillaGuardConfig guard = new ZillaGuardConfig();
                    guard.type = "jwt";
                    guard.options.issuer = "%s/realms/%s".formatted(keycloak.url, realm);
                    guard.options.audience = keycloak.audience;
                    guard.options.keys = keycloak.jwks.formatted(realm);
                    guard.options.identity = "preferred_username";

                    zilla.guards = Map.of(authnJwt, guard);
                }

                Map<String, Map<String, Map<String, String>>> httpApi = Map.of(
                    "catalog", Map.of("apicurio_catalog", Map.of("subject", HTTP_ASYNCAPI_ARTIFACT_ID,
                        "version", "latest")));
                Map<String, Map<String, Map<String, String>>> kafkaApi = Map.of(
                    "catalog", Map.of("apicurio_catalog", Map.of("subject", KAFKA_ASYNCAPI_ARTIFACT_ID,
                        "version", "latest")));

                List<ZillaBindingRouteConfig> routes = new ArrayList<>();

                for (String operation : operations)
                {
                    if (operation.endsWith("Replies"))
                    {
                        continue;
                    }
                    ZillaBindingRouteConfig route = new ZillaBindingRouteConfig();
                    route.when = List.of(Map.of("api-id", "http_api", "operation-id", operation));
                    route.with = Map.of("api-id", "kafka_api", "operation-id", suffixes.stream()
                        .filter(operation::endsWith)
                        .map(suffix -> operation.substring(0, operation.length() - suffix.length()))
                        .findFirst()
                        .orElse(operation));
                    route.exit = "south_kafka_client";
                    routes.add(route);
                }

                Map<String, ZillaBindingConfig> bindings = new HashMap<>();

                ZillaBindingConfig northHttpServer = new ZillaBindingConfig();
                northHttpServer.type = "asyncapi";
                northHttpServer.kind = "server";
                ZillaBindingOptionsConfig optionsConfig = new ZillaBindingOptionsConfig();
                optionsConfig.specs = Map.of("http_api", httpApi);
                if (realm != null)
                {
                    optionsConfig.http = new ZillaBindingOptionsConfig.HttpAuthorizationOptionsConfig();
                    optionsConfig.http.authorization = Map.of(authnJwt,
                        Map.of("credentials",
                            Map.of("headers",
                                Map.of("authorization", "Bearer {credentials}"),
                                "query", Map.of("access_token", "{credentials}"))));
                }
                northHttpServer.options = optionsConfig;
                northHttpServer.exit = "south_kafka_proxy";
                bindings.put("north_http_server", northHttpServer);

                ZillaBindingConfig southKafkaProxy = new ZillaBindingConfig();
                southKafkaProxy.type = "asyncapi";
                southKafkaProxy.kind = "proxy";
                optionsConfig = new ZillaBindingOptionsConfig();
                optionsConfig.specs = Map.of("http_api", httpApi, "kafka_api", kafkaApi);
                southKafkaProxy.options = optionsConfig;
                southKafkaProxy.routes = routes;
                bindings.put("south_kafka_proxy", southKafkaProxy);

                ZillaBindingConfig southKafkaClient = new ZillaBindingConfig();
                southKafkaClient.type = "asyncapi";
                southKafkaClient.kind = "client";
                optionsConfig = new ZillaBindingOptionsConfig();
                optionsConfig.specs = Map.of("kafka_api", kafkaApi);
                List<ZillaBindingOptionsConfig.KafkaTopicConfig> topicsConfig = new ArrayList<>();
                extractedHeaders(topicsConfig);

                ZillaBindingOptionsConfig.KafkaOptionsConfig kafkaOptionsConfig =
                    new ZillaBindingOptionsConfig.KafkaOptionsConfig();
                kafkaOptionsConfig.topics = topicsConfig;
                optionsConfig.kafka = kafkaOptionsConfig;
                southKafkaClient.options = optionsConfig;
                bindings.put("south_kafka_client", southKafkaClient);

                zilla.name = "zilla-http-kafka-asyncapi";
                zilla.catalogs = Map.of(
                    "apicurio_catalog", apicurioCatalog,
                    "karapace_catalog", karapaceCatalog);
                zilla.bindings = bindings;
                zilla.telemetry = Map.of("exporters", Map.of("stdout_logs_exporter", Map.of("type", "stdout")));

                ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                    .setSerializationInclusion(NON_NULL);

                zillaConfig = mapper.writeValueAsString(zilla);
                zillaConfig = zillaConfig.replaceAll("(:status|zilla:correlation-id)", "'$1'");

            }

            if (zillaConfig != null)
            {
                HttpRequest httpRequest = HttpRequest
                    .newBuilder(toURI("http://localhost:%d".formatted(DEFAULT_ADMIN_HTTP_PORT),
                        "/v1/config/zilla.yaml"))
                    .PUT(HttpRequest.BodyPublishers.ofString(zillaConfig))
                    .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (httpResponse.statusCode() == 204)
                {
                    System.out.println("Config Server is populated with zilla.yaml");

                    Path zillaFilesPath = Paths.get("zillabase/zilla/");

                    if (Files.exists(zillaFilesPath))
                    {
                        Files.walk(zillaFilesPath)
                            .filter(Files::isRegularFile)
                            .filter(file -> !(file.endsWith("zilla.yaml") || file.getFileName().toString().startsWith(".")))
                            .forEach(file ->
                            {
                                try
                                {
                                    Path relativePath = zillaFilesPath.relativize(file);
                                    byte[] content = Files.readAllBytes(file);
                                    HttpRequest zillaFileRequest = HttpRequest
                                        .newBuilder(toURI("http://localhost:%d".formatted(DEFAULT_ADMIN_HTTP_PORT),
                                            "/v1/config/%s".formatted(relativePath)))
                                        .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                                        .build();

                                    if (client.send(zillaFileRequest, HttpResponse.BodyHandlers.ofString()).statusCode() == 204)
                                    {
                                        System.out.println("Config Server is populated with %s".formatted(relativePath));
                                    }
                                }
                                catch (IOException | InterruptedException ex)
                                {
                                    System.err.println("Failed to process file: %s : %s".formatted(file, ex.getMessage()));
                                    ex.printStackTrace();
                                }
                            });
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    private void extractedHeaders(
        List<ZillaBindingOptionsConfig.KafkaTopicConfig> topicsConfig)
    {
        for (KafkaTopicSchemaRecord record : records)
        {
            if (record.name.endsWith("_replies"))
            {
                ZillaBindingOptionsConfig.KafkaTopicConfig topicConfig =
                    new ZillaBindingOptionsConfig.KafkaTopicConfig();
                topicConfig.name = record.name;
                ZillaBindingOptionsConfig.TransformConfig transforms =
                    new ZillaBindingOptionsConfig.TransformConfig();
                transforms.headers = Map.of(":status", "${message.value.status}",
                    "zilla:correlation-id", "${message.value.correlation_id}");
                ZillaBindingOptionsConfig.ModelConfig value = new ZillaBindingOptionsConfig.ModelConfig();
                value.model = record.type;
                value.catalog = Map.of("catalog0",
                    List.of(Map.of("subject", record.subject, "version", "latest")));
                topicConfig.value = value;
                topicConfig.transforms = List.of(transforms);
                topicsConfig.add(topicConfig);
            }
            else
            {
                String identity = record.type.equals("protobuf")
                    ? extractIdentityFieldFromProtobufSchema(record.schema)
                    : extractIdentityFieldFromSchema(record.schema);
                if (identity != null)
                {
                    ZillaBindingOptionsConfig.KafkaTopicConfig topicConfig =
                        new ZillaBindingOptionsConfig.KafkaTopicConfig();
                    topicConfig.name = record.name;
                    ZillaBindingOptionsConfig.ModelConfig value = new ZillaBindingOptionsConfig.ModelConfig();
                    value.model = record.type;
                    value.catalog = Map.of("catalog0",
                        List.of(Map.of("subject", record.subject, "version", "latest")));
                    topicConfig.value = value;
                    ZillaBindingOptionsConfig.TransformConfig transforms =
                        new ZillaBindingOptionsConfig.TransformConfig();
                    transforms.headers = Map.of("identity", "${message.value.%s}".formatted(identity));
                    topicConfig.transforms = List.of(transforms);
                    topicsConfig.add(topicConfig);
                }
            }
        }
    }

    private String extractIdentityFieldFromProtobufSchema(
        String schema)
    {
        String identity = null;
        String[] parts = schema.split(";");
        for (String part : parts)
        {
            part = part.trim();
            if (part.contains("message") || part.contains("syntax"))
            {
                continue;
            }
            String[] tokens = part.split("\\s+");
            if (tokens.length >= 2)
            {
                String fieldName = tokens[1];
                if (fieldName.endsWith("_identity"))
                {
                    identity = fieldName;
                    break;
                }
            }
        }
        return identity;
    }

    private String extractIdentityFieldFromSchema(
        String schema)
    {
        AtomicReference<String> identity = new AtomicReference<>(null);
        try
        {
            ObjectMapper schemaMapper = new ObjectMapper();
            JsonNode schemaObject = schemaMapper.readTree(schema);
            if (schemaObject.has("fields"))
            {
                JsonNode fieldsNode = schemaObject.get("fields");
                StreamSupport.stream(fieldsNode.spliterator(), false)
                    .forEach(field ->
                    {
                        String fieldName = field.has("name")
                            ? field.get("name").asText()
                            : fieldsNode.fieldNames().next();
                        if (fieldName.endsWith("_identity"))
                        {
                            identity.set(fieldName);
                        }
                    });
            }
            else if (schemaObject.has("properties"))
            {
                JsonNode fieldsNode = schemaObject.get("properties");
                fieldsNode.fieldNames().forEachRemaining(fieldName ->
                {
                    if (fieldName.endsWith("_identity"))
                    {
                        identity.set(fieldName);
                    }
                });
            }
        }
        catch (JsonProcessingException ex)
        {
        }
        return identity.get();
    }

    private void createConfigServerKafkaTopic(
        ZillabaseConfig config)
    {
        int retries = 0;
        int delay = SERVICE_INITIALIZATION_DELAY_MS;

        while (retries < MAX_RETRIES)
        {
            try
            {
                Thread.sleep(delay);
                try (AdminClient adminClient = AdminClient.create(Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafka.bootstrapUrl.equals(DEFAULT_KAFKA_BOOTSTRAP_URL)
                        ? "localhost:9092" : config.kafka.bootstrapUrl)))
                {
                    NewTopic configTopic = new NewTopic(ZILLABASE_CONFIG_KAFKA_TOPIC, 1, (short) 1);
                    configTopic.configs(Map.of("cleanup.policy", "compact"));
                    adminClient.createTopics(List.of(configTopic)).all().get();
                    break;
                }
            }
            catch (Exception ex)
            {
                retries++;
                delay *= 2;
                if (retries >= MAX_RETRIES)
                {
                    System.err.println("Error creating Zillabase Config Server topic : %s".formatted(ex.getMessage()));
                }
            }
        }
    }

    private void processAsyncApiSpecs(
        ZillabaseConfig config)
    {
        Path kafkaSpecPath = Paths.get("zillabase/specs/kafka-asyncapi.yaml");
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
            resolveKafkaTopicsAndSchemas(config);
            if (!records.isEmpty())
            {
                kafkaSpec = generateKafkaAsyncApiSpecs(config);
            }
        }

        if (kafkaSpec != null)
        {
            registerAsyncApiSpec(KAFKA_ASYNCAPI_ARTIFACT_ID, kafkaSpec);
        }

        Path httpSpecPath = Paths.get("zillabase/specs/http-asyncapi.yaml");
        String httpSpec = null;
        if (Files.exists(kafkaSpecPath))
        {
            try
            {
                httpSpec = Files.readString(httpSpecPath);
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
        else if (kafkaSpec != null)
        {
            httpSpec = generateHttpAsyncApiSpecs(config, kafkaSpec);
        }

        if (httpSpec != null)
        {
            registerAsyncApiSpec(HTTP_ASYNCAPI_ARTIFACT_ID, httpSpec);

            JsonValue jsonValue = Json.createReader(new StringReader(httpSpec)).readValue();
            JsonObject operations = jsonValue.asJsonObject().getJsonObject("operations");
            for (Map.Entry<String, JsonValue> operation : operations.entrySet())
            {
                this.operations.add(operation.getKey());
            }
        }
    }

    private void seedKafkaAndRegistry(
        ZillabaseConfig config)
    {
        KafkaBootstrapRecords records = readKafkaBootstrapRecords();
        if (records != null && !records.topics.isEmpty())
        {
            final HttpClient client = HttpClient.newBuilder()
                .version(HTTP_1_1)
                .build();

            boolean status = false;
            int retries = 0;
            int delay = SERVICE_INITIALIZATION_DELAY_MS;

            while (retries < MAX_RETRIES)
            {
                try
                {
                    Thread.sleep(delay);
                    try (AdminClient adminClient = AdminClient.create(Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafka.bootstrapUrl.equals(DEFAULT_KAFKA_BOOTSTRAP_URL)
                            ? "localhost:9092" : config.kafka.bootstrapUrl)))
                    {
                        List<NewTopic> topics = new ArrayList<>();
                        for (KafkaTopicRecord record : records.topics)
                        {
                            String name = record.name;
                            Map<String, String> topicConfig = record.config;
                            int partition = 1;
                            short replication = 1;

                            Map<String, String> configs = new HashMap<>();
                            if (topicConfig != null && !topicConfig.isEmpty())
                            {
                                for (Map.Entry<String, String> entry : topicConfig.entrySet())
                                {
                                    String key = entry.getKey();
                                    switch (key)
                                    {
                                    case "partitions":
                                        partition = Integer.parseInt(topicConfig.get("partitions"));
                                        break;
                                    case "replication_factor":
                                        replication = Short.parseShort(topicConfig.get("replication_factor"));
                                        break;
                                    default:
                                        configs.put(key, entry.getValue());
                                        break;
                                    }
                                }
                            }
                            NewTopic newTopic = new NewTopic(name, partition, replication);
                            newTopic.configs(configs);
                            topics.add(newTopic);

                            KafkaTopicSchema schema = record.schema;
                            if (schema != null)
                            {
                                if (schema.key != null)
                                {
                                    registerKafkaTopicSchema(config, client, "%s-key".formatted(name), schema.key,
                                        resolveType(schema.key));
                                }

                                if (schema.value != null)
                                {
                                    registerKafkaTopicSchema(config, client, "%s-value".formatted(name), schema.value,
                                        resolveType(schema.value));
                                }
                            }
                        }
                        status = adminClient.createTopics(topics).all().get() == null;
                        break;
                    }
                }
                catch (Exception ex)
                {
                    retries++;
                    delay *= 2;
                    if (retries >= MAX_RETRIES)
                    {
                        System.err.println("Error creating Kafka topics : %s".formatted(ex.getMessage()));
                    }
                }
            }

            if (status)
            {
                System.out.println("seed-kafka.yaml processed successfully!");
            }
            else
            {
                System.err.println("Failed to process seed-kafka.yaml");
            }

        }
    }

    private void registerKafkaTopicSchema(
        ZillabaseConfig config,
        HttpClient client,
        String subject,
        String schema,
        String schemaType) throws IOException, InterruptedException
    {
        int retries = 0;
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode idpNode = mapper.createObjectNode();
        idpNode.put("schema", schema);
        if (schemaType != null)
        {
            idpNode.put("schemaType", schemaType.toUpperCase());
        }

        HttpRequest request = HttpRequest.newBuilder(toURI(config.registry.karapace.url.equals(DEFAULT_KARAPACE_URL)
                    ? DEFAULT_CLIENT_KARAPACE_URL : config.registry.karapace.url,
                "/subjects/%s/versions".formatted(subject)))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(idpNode)))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 503 && retries == 0)
        {
            retries++;
            Thread.sleep(SERVICE_INITIALIZATION_DELAY_MS);
            registerKafkaTopicSchema(config, client, subject, schema, schemaType);
        }
        else if (response.statusCode() != 200)
        {
            System.err.println("Error registering schema for %s. Error code: %s"
                .formatted(subject, response.statusCode()));
            System.err.println(response.body());
        }

    }

    private void registerAsyncApiSpec(
        String id,
        String spec)
    {
        if (spec != null)
        {
            try
            {
                File tempFile = File.createTempFile("zillabase-asyncapi-spec", ".tmp");

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
                {
                    writer.write(spec);
                }

                ZillabaseAsyncapiAddCommand command = new ZillabaseAsyncapiAddCommand();
                command.helpOption = new HelpOption<>();
                command.spec = tempFile.getPath();
                command.id = id;
                command.run();
                tempFile.delete();
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.err);
            }
        }
    }

    private void resolveKafkaTopicsAndSchemas(
        ZillabaseConfig config)
    {
        try (AdminClient adminClient = AdminClient.create(Map.of(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.kafka.bootstrapUrl.equals(DEFAULT_KAFKA_BOOTSTRAP_URL)
                ? "localhost:9092" : config.kafka.bootstrapUrl)))
        {
            final HttpClient client = HttpClient.newHttpClient();

            KafkaFuture<Collection<TopicListing>> topics = adminClient.listTopics().listings();
            for (TopicListing topic : topics.get())
            {
                if (!topic.isInternal())
                {
                    String topicName = topic.name();

                    ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                    DescribeConfigsResult result = adminClient.describeConfigs(List.of(resource));
                    Map<ConfigResource, Config> configMap = result.all().get();

                    Config topicConfig = configMap.get(resource);
                    String[] policies = topicConfig.get(CLEANUP_POLICY_CONFIG).value().split(",");

                    String subject = "%s-value".formatted(topicName);
                    String schema = resolveSchema(config, client, subject);
                    if (schema != null)
                    {
                        JsonReader reader = Json.createReader(new StringReader(schema));
                        JsonObject object = reader.readObject();

                        if (object.containsKey("schema"))
                        {
                            String schemaStr = object.getString("schema");
                            String type = resolveType(schemaStr);
                            records.add(new KafkaTopicSchemaRecord(topicName, policies,
                                matcher.reset(topicName.replace("%s.".formatted(config.risingwave.db), ""))
                                    .replaceAll(match -> match.group(2).toUpperCase()),
                                subject, type, schemaStr));
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println("Error resolving Kafka Topics & Schemas Info");
        }
    }

    private String generateKafkaAsyncApiSpecs(
        ZillabaseConfig config)
    {
        String spec = null;
        try
        {
            final Components components = new Components();
            final Map<String, Object> schemas = new HashMap<>();
            final Map<String, Object> messages = new HashMap<>();
            final Map<String, Object> channels = new HashMap<>();
            final Map<String, Object> operations = new HashMap<>();

            Message message;
            Channel channel;
            Operation operation;
            Reference reference;

            Info info = new Info();
            info.setTitle("API Document for Kafka Cluster");
            info.setVersion("1.0.0");
            License license = new License("Aklivity Community License",
                "https://github.com/aklivity/zillabase/blob/develop/LICENSE");
            info.setLicense(license);

            Server server = new Server();
            server.setHost(config.kafka.bootstrapUrl);
            server.setProtocol("kafka");

            KafkaServerBinding kafkaServerBinding = new KafkaServerBinding();
            kafkaServerBinding.setSchemaRegistryUrl(config.registry.karapace.url);
            kafkaServerBinding.setSchemaRegistryVendor("karapace");
            server.setBindings(Map.of("kafka", kafkaServerBinding));

            for (KafkaTopicSchemaRecord record : records)
            {
                String topicName = record.name;
                String label = record.label;
                String subject = record.subject;
                String messageName = "%sMessage".formatted(label);

                String name = topicName;
                if (name.startsWith(config.risingwave.db))
                {
                    name = name.replace("%s.".formatted(config.risingwave.db), "");
                }

                channel = new Channel();
                channel.setAddress(topicName);
                KafkaChannelBinding kafkaChannelBinding = new KafkaChannelBinding();
                KafkaChannelTopicConfiguration topicConfiguration = new KafkaChannelTopicConfiguration();
                List<KafkaChannelTopicCleanupPolicy> policies = new ArrayList<>();
                for (String policy : record.cleanupPolicies)
                {
                    policies.add(KafkaChannelTopicCleanupPolicy.valueOf(policy.toUpperCase()));
                }
                topicConfiguration.setCleanupPolicy(policies);
                kafkaChannelBinding.setTopicConfiguration(topicConfiguration);
                channel.setBindings(Map.of("kafka", kafkaChannelBinding));
                reference = new Reference("#/components/messages/%s".formatted(messageName));
                channel.setMessages(Map.of(messageName, reference));
                channels.put(name, channel);

                ObjectMapper schemaMapper = new ObjectMapper();
                JsonNode schemaObject = schemaMapper.readTree(record.schema);
                if ("record".equals(schemaObject.get("type").asText()))
                {
                    ((ObjectNode) schemaObject).put("type", "object");
                }
                schemas.put(subject, schemaObject);

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
                if (name.endsWith("_commands"))
                {
                    String replyTopic = name.replace("_commands", "_replies");
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

            spec = buildAsyncApiSpec(info, components, channels, operations, Map.of("plain", server));
        }
        catch (Exception ex)
        {
            System.err.println("Error generating Kafka AsyncApi Spec");
            ex.printStackTrace(System.err);
        }
        return spec;
    }

    private String generateHttpAsyncApiSpecs(
        ZillabaseConfig config,
        String kafkaSpec)
    {
        String spec = null;
        try
        {
            final Components components = new Components();
            final Map<String, Object> schemas = new HashMap<>();
            final Map<String, Object> messages = new HashMap<>();
            final Map<String, Object> channels = new HashMap<>();
            final Map<String, Object> operations = new HashMap<>();
            final Map<String, Object> servers = new HashMap<>();

            Info info = new Info();
            info.setTitle("API Document for REST APIs");
            info.setVersion("1.0.0");
            License license = new License("Aklivity Community License",
                "https://github.com/aklivity/zillabase/blob/develop/LICENSE");
            info.setLicense(license);

            boolean secure = config.keycloak.realm != null;

            String securitySchemaName = "httpOauth";
            Reference security = new Reference("#/components/securitySchemes/%s".formatted(securitySchemaName));

            Server server = new Server();
            server.setHost("localhost:8080");
            server.setProtocol("http");
            if (secure)
            {
                server.setSecurity(List.of(security));
            }
            servers.put("http", server);

            JsonValue jsonValue = Json.createReader(new StringReader(kafkaSpec)).readValue();
            ObjectMapper schemaMapper = new ObjectMapper();

            JsonObject channelsJson = jsonValue.asJsonObject().getJsonObject("channels");
            for (Map.Entry<String, JsonValue> channelJson : channelsJson.entrySet())
            {
                String name = channelJson.getKey();
                if (name.endsWith("_replies"))
                {
                    continue;
                }

                String label = matcher.reset(name).replaceAll(match -> match.group(2).toUpperCase());
                if (secure)
                {
                    String scope = label.toLowerCase();
                    config.keycloak.scopes.add("%s:read".formatted(scope));
                    config.keycloak.scopes.add("%s:write".formatted(scope));
                }
                String messageName = "%sMessage".formatted(label);
                JsonValue channelValue = channelJson.getValue();
                Channel channel = new Channel();
                channel.setAddress("/%s".formatted(name));
                Map<String, Object> messagesRef = new HashMap<>();
                Map<String, Object> itemMessagesRef = new HashMap<>();
                for (Map.Entry<String, JsonValue> entry : channelValue.asJsonObject().getJsonObject("messages").entrySet())
                {
                    messagesRef.put(entry.getKey(), schemaMapper.readTree(entry.getValue().toString()));
                    itemMessagesRef.put(entry.getKey(), schemaMapper.readTree(entry.getValue().toString()));

                    ObjectNode arrayMessageRef = (ObjectNode) schemaMapper.readTree(entry.getValue().toString());
                    arrayMessageRef.put("$ref", "#/components/messages/" + entry.getKey() + "s");
                    messagesRef.put(entry.getKey() + "s", arrayMessageRef);
                }
                channel.setMessages(messagesRef);
                channels.put(name, channel);

                channel = new Channel();
                channel.setAddress("/%s/{id}".formatted(name));
                Parameter parameter = new Parameter();
                parameter.setDescription("Id of the item.");
                channel.setParameters(Map.of("id", parameter));
                channel.setMessages(itemMessagesRef);
                channels.put("%s-item".formatted(name), channel);

                // Channel for SSE endpoints
                channel = new Channel();
                channel.setAddress("/%s-stream".formatted(name));
                channel.setMessages(itemMessagesRef);
                channels.put("%s-stream".formatted(name), channel);

                channel = new Channel();
                channel.setAddress("/%s-stream-identity".formatted(name));
                channel.setMessages(itemMessagesRef);
                channels.put("%s-stream-identity".formatted(name), channel);

                JsonObject channelBinding = channelValue.asJsonObject().getJsonObject("bindings");
                boolean compact = false;
                if (channelBinding.containsKey("kafka"))
                {
                    JsonObject kafka = channelBinding.getJsonObject("kafka");
                    JsonObject topic = kafka.getJsonObject("topicConfiguration");
                    JsonArray policies = topic != null ? topic.getJsonArray("cleanup.policy") : null;

                    if (policies != null)
                    {
                        for (JsonValue policy : policies)
                        {
                            if (policy.getValueType() == JsonValue.ValueType.STRING &&
                                "compact".equals(policy.toString().replace("\"", "")))
                            {
                                compact = true;
                            }
                        }
                    }
                }

                generateHttpOperations(operations, secure, security, name, label, messageName, compact);
            }

            JsonObject componentsJson = jsonValue.asJsonObject().getJsonObject("components");
            JsonObject messagesJson = componentsJson.getJsonObject("messages");
            JsonObject schemasJson = componentsJson.getJsonObject("schemas");
            for (Map.Entry<String, JsonValue> messageJson : messagesJson.entrySet())
            {
                JsonNode originalMessage = schemaMapper.readTree(messageJson.getValue().toString());
                String key = messageJson.getKey();
                messages.put(key, originalMessage);

                String contentType = originalMessage.has("contentType") ?
                    originalMessage.get("contentType").asText() : "application/json";

                String arrayMessageKey = key + "s";
                ObjectNode arrayMessage = schemaMapper.createObjectNode();

                ObjectNode payloadNode = schemaMapper.createObjectNode();
                payloadNode.put("$ref", originalMessage.get("payload").get("$ref").asText() + "s");

                arrayMessage.set("payload", payloadNode);
                arrayMessage.put("contentType", contentType);
                arrayMessage.put("name", arrayMessageKey);

                messages.put(arrayMessageKey, arrayMessage);
            }

            for (Map.Entry<String, JsonValue> schemaJson : schemasJson.entrySet())
            {
                schemas.put(schemaJson.getKey(), schemaMapper.readTree(schemaJson.getValue().toString()));

                String arraySchemaKey = schemaJson.getKey() + "s";
                ObjectNode arraySchema = schemaMapper.createObjectNode();
                arraySchema.put("type", "array");

                ObjectNode itemsNode = schemaMapper.createObjectNode();
                itemsNode.put("$ref", "#/components/schemas/" + schemaJson.getKey());

                arraySchema.set("items", itemsNode);
                arraySchema.put("name",
                    arraySchemaKey.replace("%s.".formatted(config.risingwave.db), ""));
                arraySchema.put("namespace", config.risingwave.db);
                schemas.put(arraySchemaKey, arraySchema);
            }

            if (secure)
            {
                OAuth2SecurityScheme securityScheme = OAuth2SecurityScheme.oauth2Builder()
                    .flows(new OAuthFlows())
                    .scopes(config.keycloak.scopes)
                    .build();
                components.setSecuritySchemes(Map.of(securitySchemaName, securityScheme));
            }

            components.setSchemas(schemas);
            components.setMessages(messages);

            spec = buildAsyncApiSpec(info, components, channels, operations, servers);
        }
        catch (Exception ex)
        {
            System.err.println("Error generating Http AsyncApi Spec");
            ex.printStackTrace(System.err);
        }

        return spec;
    }

    private void generateHttpOperations(
        Map<String, Object> operations,
        boolean secure,
        Reference security,
        String name,
        String label,
        String messageName,
        boolean compact)
    {
        String identity = null;
        for (KafkaTopicSchemaRecord record : records)
        {
            if (label.equals(record.label))
            {
                identity = record.type.equals("protobuf")
                    ? extractIdentityFieldFromProtobufSchema(record.schema)
                    : extractIdentityFieldFromSchema(record.schema);
            }
        }

        // HTTP Operations
        Operation operation = new Operation();
        operation.setAction(OperationAction.SEND);
        Reference reference = new Reference("#/channels/%s".formatted(name));
        operation.setChannel(reference);
        reference = new Reference("#/channels/%s/messages/%s".formatted(name, messageName));
        operation.setMessages(Collections.singletonList(reference));
        HTTPOperationBinding httpBinding = new HTTPOperationBinding();
        httpBinding.setMethod(POST);
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("http", httpBinding);
        if (secure)
        {
            ZillaHttpOperationBinding zillaHttpBinding =
                new ZillaHttpOperationBinding(POST, Map.of("zilla:identity", "{identity}"), null);
            bindings.put("x-zilla-http-kafka", zillaHttpBinding);
            operation.setSecurity(List.of(security));
        }
        operation.setBindings(bindings);
        operations.put(compact ? "do%sCreate".formatted(label) : "do%s".formatted(label), operation);

        if (compact)
        {
            operation = new Operation();
            operation.setAction(OperationAction.SEND);
            reference = new Reference("#/channels/%s-item".formatted(name));
            operation.setChannel(reference);
            reference = new Reference("#/channels/%s-item/messages/%s".formatted(name, messageName));
            operation.setMessages(Collections.singletonList(reference));
            httpBinding = new HTTPOperationBinding();
            httpBinding.setMethod(PUT);
            bindings = new HashMap<>();
            bindings.put("http", httpBinding);

            if (secure)
            {
                ZillaHttpOperationBinding zillaHttpBinding = new ZillaHttpOperationBinding(PUT,
                    Map.of("zilla:identity", "{identity}"), null);
                bindings.put("x-zilla-http-kafka", zillaHttpBinding);
                operation.setSecurity(List.of(security));
            }
            operation.setBindings(bindings);
            operations.put("do%sUpdate".formatted(label), operation);
        }

        operation = new Operation();
        operation.setAction(OperationAction.RECEIVE);
        reference = new Reference("#/channels/%s".formatted(name));
        operation.setChannel(reference);
        reference = new Reference("#/channels/%s/messages/%ss".formatted(name, messageName));
        operation.setMessages(List.of(reference));
        httpBinding = new HTTPOperationBinding();
        httpBinding.setMethod(GET);
        bindings = new HashMap<>();
        bindings.put("http", httpBinding);
        if (identity != null)
        {
            AsyncapiKafkaFilter filter = new AsyncapiKafkaFilter();
            filter.headers = Map.of("identity", "{identity}");
            ZillaHttpOperationBinding zillaHttpBinding = new ZillaHttpOperationBinding(GET, null,
                List.of(filter));
            bindings.put("x-zilla-http-kafka", zillaHttpBinding);
        }
        operation.setBindings(bindings);
        if (secure)
        {
            operation.setSecurity(List.of(security));
        }
        operations.put("on%sGet".formatted(label), operation);

        operation = new Operation();
        operation.setAction(OperationAction.RECEIVE);
        reference = new Reference("#/channels/%s-item".formatted(name));
        operation.setChannel(reference);
        reference = new Reference("#/channels/%s-item/messages/%s".formatted(name, messageName));
        operation.setMessages(List.of(reference));
        operation.setBindings(bindings);
        if (secure)
        {
            operation.setSecurity(List.of(security));
        }
        operations.put("on%sGetItem".formatted(label), operation);

        // SSE Operations
        operation = new Operation();
        operation.setAction(OperationAction.RECEIVE);
        reference = new Reference("#/channels/%s-stream-identity".formatted(name));
        operation.setChannel(reference);
        reference = new Reference("#/channels/%s-stream-identity/messages/%s".formatted(name, messageName));
        operation.setMessages(List.of(reference));
        ZillaSseOperationBinding sseBinding = new ZillaSseOperationBinding();
        AsyncapiKafkaFilter filter = new AsyncapiKafkaFilter();
        if (secure)
        {
            filter.key = "{identity}";
        }
        ZillaSseKafkaOperationBinding sseKafkaBinding = new ZillaSseKafkaOperationBinding(List.of(filter));
        Map<String, Object> operationBindings = Map.of(
            "x-zilla-sse", sseBinding,
            "x-zilla-sse-kafka", sseKafkaBinding);
        operation.setBindings(operationBindings);
        if (secure)
        {
            operation.setSecurity(List.of(security));
        }
        operations.put("on%sReadItem".formatted(label), operation);


        operation = new Operation();
        operation.setAction(OperationAction.RECEIVE);
        reference = new Reference("#/channels/%s-stream".formatted(name));
        operation.setChannel(reference);
        reference = new Reference("#/channels/%s-stream/messages/%s".formatted(name, messageName));
        operation.setMessages(List.of(reference));
        bindings = new HashMap<>();
        filter = new AsyncapiKafkaFilter();
        if (identity != null)
        {
            filter.headers = Map.of("identity", "{identity}");
            sseKafkaBinding = new ZillaSseKafkaOperationBinding(List.of(filter));
            bindings.put("x-zilla-sse-kafka", sseKafkaBinding);
        }
        bindings.put("x-zilla-sse", sseBinding);
        operation.setBindings(bindings);
        if (secure)
        {
            operation.setSecurity(List.of(security));
        }
        operations.put("on%sRead".formatted(label), operation);
    }

    private String buildAsyncApiSpec(
        Info info,
        Components components,
        Map<String, Object> channels,
        Map<String, Object> operations,
        Map<String, Object> servers)
    {
        String spec = null;
        try
        {
            final AsyncAPI asyncAPI = new AsyncAPI();

            asyncAPI.setAsyncapi("3.0.0");
            asyncAPI.setInfo(info);
            asyncAPI.setServers(servers);
            asyncAPI.setComponents(components);
            asyncAPI.setChannels(channels);
            asyncAPI.setOperations(operations);

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                    .setSerializationInclusion(NON_NULL);

            spec = mapper.writeValueAsString(asyncAPI);
        }
        catch (JsonProcessingException ex)
        {
            System.err.println("Error building AsyncApi Spec");
            ex.printStackTrace(System.err);
        }
        return spec;
    }

    private String resolveType(
        String schema)
    {
        String type = null;
        try
        {
            if (protoMatcher.reset(schema.toLowerCase()).matches())
            {
                type = "protobuf";
            }
            else
            {
                ObjectMapper schemaMapper = new ObjectMapper();
                JsonNode schemaObject = schemaMapper.readTree(schema);
                if (schemaObject.has("type"))
                {
                    String schemaType = schemaObject.get("type").asText();
                    switch (schemaType)
                    {
                    case "record":
                    case "enum":
                    case "fixed":
                        type = "avro";
                        break;
                    default:
                        type = "json";
                        break;
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.err.format("Failed to parse schema type: %s:\n", ex.getMessage());
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
                .newBuilder(toURI(config.registry.karapace.url.equals(DEFAULT_KARAPACE_URL)
                        ? DEFAULT_CLIENT_KARAPACE_URL : config.registry.karapace.url,
                    "/subjects/%s/versions/latest".formatted(subject)))
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

        final ZillabaseConfig config;
        final Map<String, String> project;
        final String name;
        final String image;
        final String hostname;

        CreateContainerFactory(
            ZillabaseConfig config,
            String name,
            String image)
        {
            this.config = config;
            this.project = Map.of("com.docker.compose.project", "zillabase");
            this.name = String.format(ZILLABASE_NAME_FORMAT, name);
            this.image = image;
            this.hostname = String.format(ZILLABASE_HOSTNAME_FORMAT, name);
        }

        abstract CreateContainerCmd createContainer(
            DockerClient client);
    }

    private static final class CreateZillaFactory extends CreateContainerFactory
    {
        CreateZillaFactory(
            ZillabaseConfig config)
        {
            super(config, "zilla", "ghcr.io/aklivity/zilla:%s".formatted(config.zilla.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            List<ExposedPort> exposedPorts = config.zilla.ports.stream()
                .map(portConfig -> ExposedPort.tcp(portConfig.port))
                .toList();

            List<PortBinding> portBindings = config.zilla.ports.stream()
                .map(portConfig -> new PortBinding(Ports.Binding.bindPort(portConfig.port), ExposedPort.tcp(portConfig.port)))
                .toList();

            List<String> env = Optional.ofNullable(config.zilla.env).orElse(List.of());

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withPortBindings(portBindings)
                    .withRestartPolicy(unlessStoppedRestart()))
                .withExposedPorts(exposedPorts)
                .withCmd("start", "-v", "-e", "-c", "%s/config/zilla.yaml".formatted(config.admin.configServerUrl),
                    "-Pzilla.engine.verbose.composites=true", "-Pzilla.engine.config.poll.interval.seconds=10")
                .withTty(true)
                .withEnv(env);
        }
    }

    private static final class CreateKafkaFactory extends CreateContainerFactory
    {
        CreateKafkaFactory(
            ZillabaseConfig config)
        {
            super(config, "kafka", "bitnami/kafka:%s".formatted(config.kafka.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            ExposedPort exposedPort = ExposedPort.tcp(9092);
            String volume = "/bitnami/kafka";

            client.createVolumeCmd()
                .withName(ZILLABASE_KAFKA_VOLUME)
                .withLabels(Map.of("io.aklivity", "zillabase"))
                .exec();

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withRestartPolicy(unlessStoppedRestart())
                    .withBinds(new Bind(ZILLABASE_KAFKA_VOLUME, new Volume(volume))))
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
                    "KAFKA_CFG_LOG_DIRS=%s/logs".formatted(volume),
                    "KAFKA_CFG_PROCESS_ROLES=broker,controller",
                    "KAFKA_CFG_LISTENERS=CLIENT://:9092,INTERNAL://:29092,CONTROLLER://:9093",
                    "KAFKA_CFG_INTER_BROKER_LISTENER_NAME=INTERNAL",
                    "KAFKA_CFG_ADVERTISED_LISTENERS=CLIENT://localhost:9092,INTERNAL://kafka.zillabase.dev:29092",
                    "KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true")
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/29092")));
        }
    }

    private static final class CreateApicurioFactory extends CreateContainerFactory
    {
        CreateApicurioFactory(
            ZillabaseConfig config)
        {
            super(config, "apicurio", "apicurio/apicurio-registry-mem:%s".formatted(config.registry.apicurio.tag));
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
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/8080")));
        }
    }

    private static final class CreateKarapaceFactory extends CreateContainerFactory
    {
        CreateKarapaceFactory(
            ZillabaseConfig config)
        {
            super(config, "karapace", "ghcr.io/aiven/karapace:%s".formatted(config.registry.karapace.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            ExposedPort exposedPort = ExposedPort.tcp(8081);

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withRestartPolicy(unlessStoppedRestart()))
                .withPortBindings(new PortBinding(Ports.Binding.bindPort(8081), exposedPort))
                .withExposedPorts(exposedPort)
                .withCmd("/bin/bash", "/opt/karapace/start.sh", "registry")
                .withEnv(
                    "KARAPACE_ADVERTISED_HOSTNAME=karapace.zillabase.dev",
                    "KARAPACE_BOOTSTRAP_URI=%s".formatted(config.kafka.bootstrapUrl),
                    "KARAPACE_PORT=8081",
                    "KARAPACE_HOST=0.0.0.0",
                    "KARAPACE_CLIENT_ID=karapace",
                    "KARAPACE_GROUP_ID=karapace-registry",
                    "KARAPACE_MASTER_ELIGIBILITY=true",
                    "KARAPACE_TOPIC_NAME=_schemas",
                    "KARAPACE_LOG_LEVEL=WARNING",
                    "KARAPACE_COMPATIBILITY=FULL")
                .withTty(true)
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/8081")));
        }
    }


    private static final class CreateRisingWaveFactory extends CreateContainerFactory
    {
        CreateRisingWaveFactory(
            ZillabaseConfig config)
        {
            super(config, "risingwave", "risingwavelabs/risingwave:%s".formatted(config.risingwave.tag));
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
                .withCmd("playground")
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/4566")));
        }
    }

    private static final class CreateKeycloakFactory extends CreateContainerFactory
    {
        CreateKeycloakFactory(
            ZillabaseConfig config)
        {
            super(config, "keycloak", "bitnami/keycloak:%s".formatted(config.keycloak.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            ExposedPort exposedPort = ExposedPort.tcp(8180);

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withPortBindings(new PortBinding(Ports.Binding.bindPort(8180), exposedPort))
                    .withRestartPolicy(unlessStoppedRestart()))
                .withExposedPorts(exposedPort)
                .withEnv(
                    "KEYCLOAK_DATABASE_VENDOR=dev-file",
                    "KEYCLOAK_HTTP_PORT=8180",
                    "KEYCLOAK_ACCESS_TOKEN_LIFESPAN=3600",
                    "KEYCLOAK_ACCESS_TOKEN_LIFESPAN_IMPLICIT=3600",
                    "KEYCLOAK_ADMIN=%s".formatted(DEFAULT_KEYCLOAK_ADMIN_CREDENTIAL),
                    "KEYCLOAK_ADMIN_PASSWORD=%s".formatted(DEFAULT_KEYCLOAK_ADMIN_CREDENTIAL))
                .withTty(true)
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/8180")));
        }
    }

    private static final class CreateAuthFactory extends CreateContainerFactory
    {
        CreateAuthFactory(
            ZillabaseConfig config)
        {
            super(config, "auth", "ghcr.io/aklivity/zillabase/auth:%s".formatted(config.auth.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            List<String> envVars = Arrays.asList(
                "AUTH_SERVER_PORT=%d".formatted(DEFAULT_AUTH_PORT),
                "KEYCLOAK_REALM=%s".formatted(config.keycloak.realm),
                "DEBUG=%s".formatted(true));

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withRestartPolicy(unlessStoppedRestart()))
                .withEnv(envVars)
                .withTty(true)
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/%s".formatted(DEFAULT_AUTH_PORT))));
        }
    }

    private static final class CreateAdminFactory extends CreateContainerFactory
    {
        CreateAdminFactory(
            ZillabaseConfig config)
        {
            super(config, "admin", "ghcr.io/aklivity/zilla:%s".formatted(config.admin.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            List<ExposedPort> exposedPorts = config.admin.PORTS.stream()
                .map(port -> ExposedPort.tcp(port))
                .toList();

            List<PortBinding> portBindings = config.admin.PORTS.stream()
                .map(port -> new PortBinding(Ports.Binding.bindPort(port), ExposedPort.tcp(port)))
                .toList();

            URI apicurio = URI.create(config.registry.apicurio.url);
            URI configServer = URI.create(config.admin.configServerUrl);
            String risingwaveUrl = config.risingwave.url;
            String[] risingwave = risingwaveUrl.split(":");

            List<String> envVars = Arrays.asList(
                "ZILLA_INCUBATOR_ENABLED=%s".formatted(true),
                "RISINGWAVE_HOST=%s".formatted(risingwave[0]),
                "RISINGWAVE_PORT=%s".formatted(risingwave[1]),
                "CONFIG_SERVER_HOST=%s".formatted(configServer.getHost()),
                "CONFIG_SERVER_PORT=%d".formatted(configServer.getPort()),
                "APICURIO_HOST=%s".formatted(apicurio.getHost()),
                "APICURIO_PORT=%d".formatted(apicurio.getPort()),
                "REGISTRY_GROUP_ID=%s".formatted(config.registry.apicurio.groupId),
                "AUTH_ADMIN_HOST=%s".formatted(DEFAULT_AUTH_HOST),
                "AUTH_ADMIN_PORT=%d".formatted(DEFAULT_AUTH_PORT),
                "KARAPACE_URL=%s".formatted(config.registry.karapace.url),
                "KAFKA_BOOTSTRAP_SERVER=%s".formatted(config.kafka.bootstrapUrl),
                "UDF_JAVA_SERVER=%s".formatted("http://udf-server-java.zillabase.dev:8815"),
                "UDF_PYTHON_SERVER=%s".formatted("http://udf-server-python.zillabase.dev:8816"));

            CreateContainerCmd container = client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withPortBindings(portBindings)
                    .withRestartPolicy(unlessStoppedRestart()))
                .withCmd("start", "-v", "-e")
                .withExposedPorts(exposedPorts)
                .withEnv(envVars)
                .withTty(true)
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/7184")));


            try
            {
                File tempFile = File.createTempFile("zillabase-admin-server-zilla", ".yaml");
                Path configPath = Paths.get(tempFile.getPath());
                Files.writeString(configPath, ZILLABASE_ADMIN_SERVER_ZILLA_YAML);
                container.withBinds(new Bind(configPath.toAbsolutePath().toString(), new Volume("/etc/zilla/zilla.yaml")),
                    new Bind("/var/storage", new Volume("/var/storage")));
                tempFile.deleteOnExit();
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.err);
            }
            return container;
        }
    }

    private static final class CreateConfigFactory extends CreateContainerFactory
    {
        CreateConfigFactory(
            ZillabaseConfig config)
        {
            super(config, "config", "ghcr.io/aklivity/zilla:%s".formatted(config.admin.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            List<String> envVars = Arrays.asList(
                "KAFKA_BOOTSTRAP_SERVER=%s".formatted(config.kafka.bootstrapUrl));

            CreateContainerCmd container = client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withRestartPolicy(unlessStoppedRestart()))
                .withCmd("start", "-v", "-e")
                .withEnv(envVars)
                .withTty(true)
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/7114")));

            try
            {
                File tempFile = File.createTempFile("zillabase-config-server-zilla", ".yaml");
                Path configPath = Paths.get(tempFile.getPath());
                Files.writeString(configPath, ZILLABASE_CONFIG_SERVER_ZILLA_YAML);
                container.withBinds(new Bind(configPath.toAbsolutePath().toString(), new Volume("/etc/zilla/zilla.yaml")));
                tempFile.deleteOnExit();
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.err);
            }
            return container;
        }
    }

    private static final class CreateUdfServerJavaFactory extends CreateContainerFactory
    {
        CreateUdfServerJavaFactory(
            ZillabaseConfig config)
        {
            super(config, "udf-server-java", "ghcr.io/aklivity/zillabase/udf-server-java:%s".formatted(config.udf.java.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            List<String> envVars = new ArrayList<>();
            envVars.add("CLASSPATH=udf-server.jar:/opt/udf/lib/*");

            List<String> env = config.udf.java.env;
            if (env != null)
            {
                envVars.addAll(env);
            }

            String projectsBasePath = "zillabase/functions/java";

            File projectsDirectory = new File(projectsBasePath);
            List<Bind> binds = new ArrayList<>();

            if (projectsDirectory.exists() && projectsDirectory.isDirectory())
            {
                File targetDir = new File(projectsDirectory, "target");
                if (targetDir.exists())
                {
                    File[] jarFiles = targetDir.listFiles((dir, name) -> name.endsWith(".jar"));
                    if (jarFiles != null)
                    {
                        for (File jarFile : jarFiles)
                        {
                            String jarHostPath = jarFile.getAbsolutePath();
                            String jarContainerPath = "/opt/udf/lib/" + jarFile.getName();
                            binds.add(new Bind(jarHostPath, new Volume(jarContainerPath)));
                        }
                    }
                }
            }

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withBinds(binds)
                    .withRestartPolicy(unlessStoppedRestart()))
                .withEnv(envVars)
                .withTty(true)
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/8815")));
        }
    }

    private static final class CreateUdfServerPythonFactory extends CreateContainerFactory
    {
        CreateUdfServerPythonFactory(
            ZillabaseConfig config)
        {
            super(config, "udf-server-python",
                    "ghcr.io/aklivity/zillabase/udf-server-python:%s".formatted(config.udf.python.tag));
        }

        private void importModule(
            File directory,
            List<Bind> binds)
        {
            File[] files = directory.listFiles();
            if (files != null)
            {
                for (File file : files)
                {
                    if (file.isDirectory())
                    {
                        importModule(file, binds);
                    }
                    else if (file.isFile() && file.getName().endsWith(".py"))
                    {
                        String pyHostPath = file.getAbsolutePath();
                        String pyContainerPath = "/opt/udf/lib/" + file.getName();
                        binds.add(new Bind(pyHostPath, new Volume(pyContainerPath)));
                    }
                }
            }
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            String projectsBasePath = "zillabase/functions/python";

            File projectsDirectory = new File(projectsBasePath);
            List<Bind> binds = new ArrayList<>();

            if (projectsDirectory.exists() && projectsDirectory.isDirectory())
            {
                importModule(projectsDirectory, binds);

                File requirementsFile = new File(projectsDirectory, "requirements.txt");
                if (requirementsFile.exists() && requirementsFile.isFile())
                {
                    String requirementsHostPath = requirementsFile.getAbsolutePath();
                    String requirementsContainerPath = "/opt/udf/lib/requirements.txt";
                    binds.add(new Bind(requirementsHostPath, new Volume(requirementsContainerPath)));
                }

                File[] projectDirs = projectsDirectory.listFiles(File::isDirectory);
                if (projectDirs != null)
                {
                    for (File projectDir : projectDirs)
                    {
                        importModule(projectDir, binds);
                    }
                }
            }

            List<String> env = Optional.ofNullable(config.udf.python.env).orElse(List.of());

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withBinds(binds))
                .withTty(true)
                .withEnv(env)
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(5L))
                    .withTimeout(SECONDS.toNanos(3L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/8816")));
        }
    }

    private final class PgsqlHelper
    {
        private final ZillabaseRisingWaveConfig config;
        private final String url;
        private final Properties props;

        private boolean connected;


        PgsqlHelper(
            ZillabaseRisingWaveConfig config)
        {
            this.config = config;

            Properties props = new Properties();
            props.setProperty("user", "root");
            props.setProperty("preferQueryMode", PreferQueryMode.SIMPLE.value());

            this.url = "jdbc:postgresql://localhost:4567/%s".formatted(config.db);
            this.props = props;
        }

        void connect()
        {
            int retries = 0;
            int delay = SERVICE_INITIALIZATION_DELAY_MS;

            while (retries < MAX_RETRIES)
            {
                try
                {
                    Thread.sleep(delay);

                    try (Connection conn = DriverManager.getConnection(url, props);
                         Statement stmt = conn.createStatement())
                    {
                        connected = true;
                        break;
                    }
                }
                catch (InterruptedException | SQLException ex)
                {
                    retries++;
                    delay *= 2;
                }
            }

            if (!connected)
            {
                System.err.println("Failed to connect to localhost:4567 after " + MAX_RETRIES + " attempts.");
            }
        }

        void process(
            Path sql)
        {
            String content = readSql(sql);
            if (content != null)
            {
                execute(sql.getFileName().toString(), content);
            }
        }

        private void execute(
            String filename,
            String content)
        {
            try
            {
                Connection conn = DriverManager.getConnection(url, props);
                Statement stmt = conn.createStatement();
                // Set the timeout in seconds (for example, 30 seconds)
                stmt.setQueryTimeout(30);
                String noCommentsSQL = content.replaceAll("(?s)/\\*.*?\\*/", "")
                        .replaceAll("--.*?(\\r?\\n)", "");

                List<String> splitCommands = splitSQL(noCommentsSQL);

                for (String command : splitCommands)
                {
                    if (!command.trim().isEmpty())
                    {
                        command = command.trim().replaceAll("[\\n\\r]+$", "");
                        System.out.println("Executing command: " + command);
                        stmt.executeUpdate(command);
                    }
                }
            }
            catch (SQLException ex)
            {
                connected = false;
                System.out.format("Failed to process %s. ex: %s\n", filename, ex.getMessage());
            }

            if (connected)
            {
                System.out.format("%s processed successfully\n", filename);
            }
        }

        private String readSql(
            Path seedPath)
        {
            String content = null;
            try
            {
                if (Files.exists(seedPath) &&
                    Files.size(seedPath) != 0 &&
                    Files.readAllLines(seedPath).stream()
                        .anyMatch(line -> !line.trim().isEmpty() && !line.trim().startsWith("--")))
                {
                    content = Files.readString(seedPath);
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace(System.err);
            }
            return content;
        }

        private static List<String> splitSQL(
            String sql)
        {
            List<String> result = new ArrayList<>();
            StringBuilder command = new StringBuilder();
            boolean insideDollarBlock = false;

            String[] lines = sql.split("\\r?\\n");

            for (String line : lines)
            {
                if (line.contains("$$"))
                {
                    insideDollarBlock = !insideDollarBlock;
                }

                command.append(line).append("\n");

                if (!insideDollarBlock && line.trim().endsWith(";"))
                {
                    result.add(command.toString().trim());
                    command.setLength(0);
                }
            }

            if (!command.isEmpty())
            {
                result.add(command.toString().trim());
            }

            return result;
        }
    }
}
