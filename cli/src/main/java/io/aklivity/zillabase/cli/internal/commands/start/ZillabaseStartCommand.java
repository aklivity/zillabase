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
import static io.aklivity.zillabase.cli.config.ZillabaseConfigServerConfig.ZILLABASE_CONFIG_KAFKA_TOPIC;
import static io.aklivity.zillabase.cli.config.ZillabaseConfigServerConfig.ZILLABASE_CONFIG_SERVER_ZILLA_YAML;
import static io.aklivity.zillabase.cli.config.ZillabaseKafkaConfig.DEFAULT_KAFKA_BOOTSTRAP_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseKarapaceConfig.DEFAULT_CLIENT_KARAPACE_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseKarapaceConfig.DEFAULT_KARAPACE_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseRisingWaveConfig.DEFAULT_RISINGWAVE_PORT;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;
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
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaReader;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;

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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakClientConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakConfig;
import io.aklivity.zillabase.cli.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.cli.internal.asyncapi.ZillaHttpOperationBinding;
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

@Command(
    name = "start",
    description = "Start containers for local development")
public final class ZillabaseStartCommand extends ZillabaseDockerCommand
{
    private static final int SERVICE_INITIALIZATION_DELAY_MS = 5000;
    private static final int MAX_RETRIES = 5;
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(^|-)(.)");
    private static final String DEFAULT_KEYCLOAK_ADMIN_CREDENTIAL = "admin";
    private static final String ADMIN_REALMS_PATH = "/admin/realms";
    private static final String ADMIN_REALMS_CLIENTS_PATH = "/admin/realms/%s/clients";
    private static final String ADMIN_REALMS_CLIENTS_SCOPE_PATH = "/admin/realms/%s/clients/%s/default-client-scopes/%s";
    private static final String ADMIN_REALMS_SCOPE_PATH = "/admin/realms/%s/client-scopes";
    private static final Pattern EXPRESSION_PATTERN =
        Pattern.compile("\\$\\{\\{\\s*([^\\s\\}]*)\\.([^\\s\\}]*)\\s*\\}\\}");

    private final Matcher matcher = TOPIC_PATTERN.matcher("");
    private final List<String> operations = new ArrayList<>();

    public String kafkaSeedFilePath = "zillabase/seed-kafka.yaml";
    private Matcher envMatcher = EXPRESSION_PATTERN.matcher("");

    private String kafkaArtifactId;
    private String httpArtifactId;

    @Override
    protected void invoke(
        DockerClient client)
    {
        final ZillabaseConfig config = readZillabaseConfig();

        new CreateNetworkFactory().createNetwork(client);

        List<CreateContainerFactory> factories = new LinkedList<>();
        factories.add(new CreateAdminFactory(config));
        factories.add(new CreateConfigFactory(config));
        factories.add(new CreateZillaFactory(config));
        factories.add(new CreateKafkaFactory(config));
        factories.add(new CreateRisingWaveFactory(config));
        factories.add(new CreateApicurioFactory(config));
        factories.add(new CreateKeycloakFactory(config));
        factories.add(new CreateKarapaceFactory(config));

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
                String responseId = response.getId();
                containerIds.add(responseId);
            }
        }

        for (String containerId : containerIds)
        {
            try (StartContainerCmd command = client.startContainerCmd(containerId))
            {
                command.exec();
            }
        }

        seedKafkaAndRegistry(config);

        seedSql(config);

        createConfigServerKafkaTopic(config);

        processAsyncApiSpecs(config);

        initializeKeycloakService(config);

        publishZillaConfig(config);
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
                createKeycloakClientScope(config, client, url, token, realm);
                createKeycloakClient(config, client, url, token);
            }
            else
            {
                System.out.println("Failed to initialize Keycloak Service");
            }
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

    private String readSeedSql()
    {
        String content = null;
        Path seedPath = Paths.get("zillabase/seed.sql");
        try
        {
            if (Files.exists(seedPath) && Files.size(seedPath) != 0 && Files.readAllLines(seedPath)
                .stream().anyMatch(line -> !line.trim().isEmpty() && !line.trim().startsWith("--")))
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

    private ZillabaseConfig readZillabaseConfig()
    {
        ZillabaseConfig config;

        Path configPath = Paths.get("zillabase/config.yaml");
        try
        {
            if (Files.size(configPath) == 0 || Files.readAllLines(configPath)
                .stream().allMatch(line -> line.trim().isEmpty() || line.trim().startsWith("#")))
            {
                config = new ZillabaseConfig();
            }
            else
            {
                try (InputStream inputStream = Files.newInputStream(configPath);
                     InputStream schemaStream = getClass().getResourceAsStream("/internal/schema/zillabase.schema.json"))
                {
                    JsonProvider schemaProvider = JsonProvider.provider();
                    JsonReader schemaReader = schemaProvider.createReader(schemaStream);
                    JsonObject schemaObject = schemaReader.readObject();

                    JsonParser schemaParser = schemaProvider.createParserFactory(null)
                        .createParser(new StringReader(schemaObject.toString()));

                    JsonValidationService service = JsonValidationService.newInstance();
                    JsonSchemaReader reader = service.createSchemaReader(schemaParser);
                    JsonSchema schema = reader.read();

                    JsonProvider provider = service.createJsonProvider(schema, parser -> ProblemHandler.throwing());

                    Jsonb jsonb = JsonbBuilder.newBuilder()
                        .withProvider(provider)
                        .build();
                    config = jsonb.fromJson(inputStream, ZillabaseConfig.class);
                }
            }
        }
        catch (IOException | JsonbException ex)
        {
            System.err.println("Error resolving config, reverting to default.");
            ex.printStackTrace(System.err);
            config = new ZillabaseConfig();
        }

        return config;
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
                List<String> suffixes = Arrays.asList("ReadItem", "Update", "Read", "Create", "Delete");

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


                    zilla.guards = Map.of(authnJwt, guard);
                }

                Map<String, Map<String, Map<String, String>>> httpApi = Map.of(
                    "catalog", Map.of("apicurio_catalog", Map.of("subject", httpArtifactId, "version", "latest")));
                Map<String, Map<String, Map<String, String>>> kafkaApi = Map.of(
                    "catalog", Map.of("apicurio_catalog", Map.of("subject", kafkaArtifactId, "version", "latest")));

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
                                Map.of("authorization", "Bearer {credentials}"))));
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
                southKafkaClient.options = optionsConfig;
                bindings.put("south_kafka_client", southKafkaClient);

                zilla.name = "zilla-http-kafka-asyncapi";
                zilla.catalogs = Map.of(
                    "apicurio_catalog", apicurioCatalog,
                    "karapace_catalog", karapaceCatalog);
                zilla.bindings = bindings;

                ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                        .setSerializationInclusion(NON_NULL);

                zillaConfig = mapper.writeValueAsString(zilla);
            }

            if (zillaConfig != null)
            {
                HttpRequest httpRequest = HttpRequest
                    .newBuilder(toURI("http://localhost:%d".formatted(config.admin.port), "/v1/config/zilla.yaml"))
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
                                        .newBuilder(toURI("http://localhost:%d".formatted(config.admin.port),
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
            List<KafkaTopicSchemaRecord> records = resolveKafkaTopicsAndSchemas(config);
            if (!records.isEmpty())
            {
                kafkaSpec = generateKafkaAsyncApiSpecs(config, records);
            }
        }

        CRC32C crc32c = new CRC32C();
        if (kafkaSpec != null)
        {
            byte[] kafkaSpecBytes = kafkaSpec.getBytes();
            crc32c.update(kafkaSpecBytes, 0, kafkaSpecBytes.length);
            kafkaArtifactId = "zillabase-asyncapi-%s".formatted(crc32c.getValue());

            registerAsyncApiSpec(kafkaSpec);
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
            crc32c.reset();
            byte[] httpSpecBytes = httpSpec.getBytes();
            crc32c.update(httpSpecBytes, 0, httpSpecBytes.length);
            httpArtifactId = "zillabase-asyncapi-%s".formatted(crc32c.getValue());

            registerAsyncApiSpec(httpSpec);

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
                                Thread.sleep(delay);
                                if (schema.key != null)
                                {
                                    registerKafkaTopicSchema(config, client, "%s-key".formatted(name), schema.key, "AVRO");
                                }

                                if (schema.value != null)
                                {
                                    registerKafkaTopicSchema(config, client, "%s-value".formatted(name), schema.value, "AVRO");
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
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode idpNode = mapper.createObjectNode();
        idpNode.put("schema", schema);
        idpNode.put("schemaType", schemaType);

        HttpRequest request = HttpRequest.newBuilder(toURI(config.registry.karapace.url.equals(DEFAULT_KARAPACE_URL)
                    ? DEFAULT_CLIENT_KARAPACE_URL : config.registry.karapace.url,
                "/subjects/%s/versions".formatted(subject)))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(idpNode)))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
        {
            System.err.println("Error registering schema for %s. Error code: %s"
                .formatted(subject, response.statusCode()));
            System.err.println(response.body());
        }
    }

    private void registerAsyncApiSpec(
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

                System.out.println("Registering zillabase-asyncapi spec");
                ZillabaseAsyncapiAddCommand command = new ZillabaseAsyncapiAddCommand();
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
                                matcher.reset(topicName).replaceAll(match -> match.group(2).toUpperCase()),
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
        return records;
    }

    private String generateKafkaAsyncApiSpecs(
        ZillabaseConfig config,
        List<KafkaTopicSchemaRecord> records)
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
                String name = record.name;
                String label = record.label;
                String subject = record.subject;
                String messageName = "%sMessage".formatted(label);

                channel = new Channel();
                channel.setAddress(name);
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
                if (name.endsWith("-replies"))
                {
                    continue;
                }
                String label = matcher.reset(name).replaceAll(match -> match.group(2).toUpperCase());
                if (secure)
                {
                    config.keycloak.scopes.add("%s:read".formatted(label));
                    config.keycloak.scopes.add("%s:write".formatted(label));
                }
                String messageName = "%sMessage".formatted(label);
                JsonValue channelValue = channelJson.getValue();
                Channel channel = new Channel();
                channel.setAddress("/%s".formatted(name));
                Map<String, Object> messagesRef = new HashMap<>();
                for (Map.Entry<String, JsonValue> entry : channelValue.asJsonObject().getJsonObject("messages").entrySet())
                {
                    messagesRef.put(entry.getKey(), schemaMapper.readTree(entry.getValue().toString()));
                }
                channel.setMessages(messagesRef);
                channels.put(name, channel);

                channel = new Channel();
                channel.setAddress("/%s/{id}".formatted(name));
                Parameter parameter = new Parameter();
                parameter.setDescription("Id of the item.");
                channel.setParameters(Map.of("id", parameter));
                channel.setMessages(messagesRef);
                channels.put("%s-item".formatted(name), channel);

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
                messages.put(messageJson.getKey(), schemaMapper.readTree(messageJson.getValue().toString()));
            }

            for (Map.Entry<String, JsonValue> schemaJson : schemasJson.entrySet())
            {
                schemas.put(schemaJson.getKey(), schemaMapper.readTree(schemaJson.getValue().toString()));
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
        Operation operation = new Operation();
        operation.setAction(OperationAction.SEND);
        Reference reference = new Reference("#/channels/%s".formatted(name));
        operation.setChannel(reference);
        reference = new Reference("#/channels/%s/messages/%s".formatted(name, messageName));
        operation.setMessages(Collections.singletonList(reference));
        HTTPOperationBinding httpBinding = new HTTPOperationBinding();
        httpBinding.setMethod(POST);
        ZillaHttpOperationBinding zillaHttpBinding =
            new ZillaHttpOperationBinding(POST, Map.of("zilla:identity", "{identity}"));
        Map<String, Object> bindings = new HashMap<>();
        bindings.put("http", httpBinding);
        //bindings.put("x-zilla-http-kafka", zillaHttpBinding);
        operation.setBindings(bindings);
        if (secure)
        {
            operation.setSecurity(List.of(security));
        }
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
            zillaHttpBinding = new ZillaHttpOperationBinding(PUT, Map.of("zilla:identity", "{identity}"));
            bindings = new HashMap<>();
            bindings.put("http", httpBinding);
            //bindings.put("x-zilla-http-kafka", zillaHttpBinding);
            operation.setBindings(bindings);
            if (secure)
            {
                operation.setSecurity(List.of(security));
            }
            operations.put("do%sUpdate".formatted(label), operation);
        }

        operation = new Operation();
        operation.setAction(OperationAction.RECEIVE);
        reference = new Reference("#/channels/%s".formatted(name));
        operation.setChannel(reference);
        reference = new Reference("#/channels/%s/messages/%s".formatted(name, messageName));
        operation.setMessages(Collections.singletonList(reference));
        httpBinding = new HTTPOperationBinding();
        httpBinding.setMethod(GET);
        ZillaSseOperationBinding sseBinding = new ZillaSseOperationBinding(GET);
        operation.setBindings(Map.of("http", httpBinding, "x-zilla-sse", sseBinding));
        if (secure)
        {
            operation.setSecurity(List.of(security));
        }
        operations.put("on%sRead".formatted(label), operation);

        operation = new Operation();
        operation.setAction(OperationAction.RECEIVE);
        reference = new Reference("#/channels/%s-item".formatted(name));
        operation.setChannel(reference);
        reference = new Reference("#/channels/%s-item/messages/%s".formatted(name, messageName));
        operation.setMessages(Collections.singletonList(reference));
        httpBinding = new HTTPOperationBinding();
        httpBinding.setMethod(GET);
        sseBinding = new ZillaSseOperationBinding(GET);
        operation.setBindings(Map.of("http", httpBinding, "x-zilla-sse", sseBinding));
        if (secure)
        {
            operation.setSecurity(List.of(security));
        }
        operations.put("on%sReadItem".formatted(label), operation);
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
            ObjectMapper schemaMapper = new ObjectMapper();
            JsonNode schemaObject = schemaMapper.readTree(schema);
            String schemaType = schemaObject.get("type").asText();
            switch (schemaType)
            {
            case "record":
            case "enum":
            case "fixed":
                type = "avro";
                break;
            case "object":
            case "array":
                type = "json";
                break;
            default:
                type = schemaType;
                break;
            }
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
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

    private void seedSql(
        ZillabaseConfig config)
    {
        String content = readSeedSql();
        if (content != null)
        {
            Properties props = new Properties();
            props.setProperty("user", "root");

            boolean status = false;
            int retries = 0;
            int delay = SERVICE_INITIALIZATION_DELAY_MS;

            while (retries < MAX_RETRIES)
            {
                try
                {
                    Thread.sleep(delay);
                    try (Connection conn = DriverManager.getConnection("jdbc:postgresql://%s/%s"
                        .formatted(config.risingwave.url, config.risingwave.db), props);
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

            if (status)
            {
                System.out.println("seed.sql processed successfully!");
            }
            else
            {
                System.err.println("Failed to process seed.sql after " + MAX_RETRIES + " attempts.");
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


            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                        .withNetworkMode(network)
                        .withPortBindings(portBindings))
                .withExposedPorts(exposedPorts)
                .withCmd("start", "-v", "-e", "-c", "%s/config/zilla.yaml".formatted(config.admin.configServerUrl))
                .withTty(true);
        }
    }

    private static final class CreateKafkaFactory extends CreateContainerFactory
    {
        CreateKafkaFactory(
            ZillabaseConfig config)
        {
            super(config, "kafka", "bitnami/kafka:3.2.3");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
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
                    "KAFKA_CFG_ADVERTISED_LISTENERS=CLIENT://localhost:9092,INTERNAL://kafka.zillabase.dev:29092",
                    "KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true");
        }
    }

    private static final class CreateApicurioFactory extends CreateContainerFactory
    {
        CreateApicurioFactory(
            ZillabaseConfig config)
        {
            super(config, "apicurio", "apicurio/apicurio-registry-mem:latest-release");
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

    private static final class CreateKarapaceFactory extends CreateContainerFactory
    {
        CreateKarapaceFactory(
            ZillabaseConfig config)
        {
            super(config, "karapace", "ghcr.io/aiven/karapace:latest");
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
                .withTty(true);
        }
    }


    private static final class CreateRisingWaveFactory extends CreateContainerFactory
    {
        CreateRisingWaveFactory(
            ZillabaseConfig config)
        {
            super(config, "risingwave", "risingwavelabs/risingwave:latest");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
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
        CreateKeycloakFactory(
            ZillabaseConfig config)
        {
            super(config, "keycloak", "bitnami/keycloak:latest");
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
                .withTty(true)
                .withEnv(
                    "KEYCLOAK_DATABASE_VENDOR=dev-file",
                    "KEYCLOAK_HTTP_PORT=8180",
                    "KEYCLOAK_ADMIN=%s".formatted(DEFAULT_KEYCLOAK_ADMIN_CREDENTIAL),
                    "KEYCLOAK_ADMIN_PASSWORD=%s".formatted(DEFAULT_KEYCLOAK_ADMIN_CREDENTIAL));
        }
    }

    private static final class CreateAdminFactory extends CreateContainerFactory
    {
        CreateAdminFactory(
            ZillabaseConfig config)
        {
            super(config, "admin", "ghcr.io/aklivity/zillabase/admin:%s".formatted(config.admin.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            List<String> envVars = Arrays.asList(
                "ADMIN_PORT=%d".formatted(config.admin.port),
                "REGISTRY_URL=%s".formatted(config.registry.apicurio.url),
                "REGISTRY_GROUP_ID=%s".formatted(config.registry.apicurio.groupId),
                "CONFIG_SERVER_URL=%s".formatted(config.admin.configServerUrl),
                "DEBUG=%s".formatted(true));

            int port = config.admin.port;
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

    private static final class CreateConfigFactory extends CreateContainerFactory
    {
        CreateConfigFactory(
            ZillabaseConfig config)
        {
            super(config, "config", "ghcr.io/aklivity/zilla:%s".formatted(config.zilla.tag));
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
                    .withNetworkMode(network))
                .withCmd("start", "-v", "-e")
                .withEnv(envVars)
                .withTty(true);

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
}
