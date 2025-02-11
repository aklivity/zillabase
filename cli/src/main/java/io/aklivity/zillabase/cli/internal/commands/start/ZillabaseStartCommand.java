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
import static io.aklivity.zillabase.cli.config.ZillabaseAdminConfig.DEFAULT_ADMIN_HTTP_PORT;
import static io.aklivity.zillabase.cli.config.ZillabaseAdminConfig.ZILLABASE_ADMIN_SERVER_ZILLA_YAML;
import static io.aklivity.zillabase.cli.config.ZillabaseApicurioConfig.DEFAULT_APICURIO_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseAuthConfig.DEFAULT_AUTH_HOST;
import static io.aklivity.zillabase.cli.config.ZillabaseAuthConfig.DEFAULT_AUTH_PORT;
import static io.aklivity.zillabase.cli.config.ZillabaseConfigServerConfig.ZILLABASE_API_GEN_EVENTS_KAFKA_TOPIC;
import static io.aklivity.zillabase.cli.config.ZillabaseConfigServerConfig.ZILLABASE_CONFIG_KAFKA_TOPIC;
import static io.aklivity.zillabase.cli.config.ZillabaseConfigServerConfig.ZILLABASE_CONFIG_SERVER_ZILLA_YAML;
import static io.aklivity.zillabase.cli.config.ZillabaseKafkaConfig.DEFAULT_KAFKA_BOOTSTRAP_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseKarapaceConfig.DEFAULT_CLIENT_KARAPACE_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseKarapaceConfig.DEFAULT_KARAPACE_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseRisingWaveConfig.DEFAULT_RISINGWAVE_URL;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.apache.kafka.clients.admin.NewTopic;
import org.fusesource.jansi.Ansi;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaReader;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;
import org.postgresql.jdbc.PreferQueryMode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakClientConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakUserConfig;
import io.aklivity.zillabase.cli.config.ZillabaseRisingWaveConfig;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseDockerCommand;
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

    private static final String ZILLABASE_KAFKA_VOLUME_NAME = "zillabase_kafka";
    private static final String ZILLABASE_POSTGRES_VOLUME_NAME = "zillabase_postgres";
    private static final String ZILLABASE_MINIO_VOLUME_NAME = "zillabase_minio";
    private static final String ZILLABASE_UDF_PYTHON_VOLUME_NAME = "zillabase_udf_python";

    public static final String PROJECT_NAME = ZILLABASE_PATH.toAbsolutePath().getParent().getFileName().toString();
    public static final String VOLUME_LABEL = "io.aklivity.zillabase.cli.project";

    private final Matcher envMatcher = EXPRESSION_PATTERN.matcher("");
    private final Matcher protoMatcher = PROTO_MESSAGE_PATTERN.matcher("");
    private final Path seedSqlPath = ZILLABASE_PATH.resolve("seed.sql");

    public String kafkaSeedFilePath = "zillabase/seed-kafka.yaml";

    @Override
    protected void invoke(
        DockerClient client)
    {
        final ZillabaseConfig config = readZillabaseConfig();

        startContainers(client, config);

        createConfigServerKafkaTopic(config);

        seedKafkaAndRegistry(config);

        initializeKeycloakService(config);

        processInitSql(config);

        processSql(config);

        processSystemSql(config);
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
        factories.add(new CreateMinioFactory(config));
        factories.add(new CreatePostgresFactory(config));
        factories.add(new CreateApiGenFactory(config));

        if (config.kafka.bootstrapUrl.equals(DEFAULT_KAFKA_BOOTSTRAP_URL))
        {
            factories.add(new CreateKafkaFactory(config));
        }

        if (config.risingwave.url.equals(DEFAULT_RISINGWAVE_URL))
        {
            factories.add(new CreateRisingWaveFactory(config));
        }

        if (config.registry.apicurio.url.equals(DEFAULT_APICURIO_URL))
        {
            factories.add(new CreateApicurioFactory(config));
        }

        if (config.keycloak.realm != null)
        {
            factories.add(new CreateKeycloakFactory(config));
        }

        if (config.registry.karapace.url.equals(DEFAULT_KARAPACE_URL))
        {
            factories.add(new CreateKarapaceFactory(config));
        }

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

        System.out.println("Started containers successfully, awaiting health checks");

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

    private void processInitSql(
        ZillabaseConfig config)
    {
        PgsqlHelper pgsql = new PgsqlHelper(config.risingwave, "postgres");

        pgsql.connect();

        if (pgsql.connected)
        {
            pgsql.process("<initdb>",
                """
                CREATE USER zillabase;
                CREATE SCHEMA zb_catalog AUTHORIZATION postgres;
                CREATE TABLE zb_catalog.zviews(
                    name VARCHAR PRIMARY KEY,
                    sql VARCHAR);
                CREATE TABLE zb_catalog.ztables(
                    name VARCHAR PRIMARY KEY,
                    sql VARCHAR);
                CREATE TABLE zb_catalog.zfunctions(
                    name VARCHAR PRIMARY KEY,
                    sql VARCHAR);
                """);
        }
    }

    private void processSystemSql(
        ZillabaseConfig config)
    {
        PgsqlHelper pgsql = new PgsqlHelper(config.risingwave, "postgres");

        pgsql.connect();

        if (pgsql.connected)
        {
            pgsql.process("<systemdb>",
                """
                CREATE ZVIEW zcatalogs AS
                  SELECT name AS source_id FROM "zb_catalog"."zviews"
                  UNION ALL
                  SELECT name AS source_id FROM "zb_catalog"."ztables";
                """);
        }
    }

    private void processSql(
        ZillabaseConfig config)
    {
        PgsqlHelper pgsql = new PgsqlHelper(config.risingwave, "zillabase");

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
                    NewTopic eventTopic = new NewTopic(ZILLABASE_API_GEN_EVENTS_KAFKA_TOPIC, 1, (short) 1);
                    eventTopic.configs(Map.of("cleanup.policy", "delete"));
                    adminClient.createTopics(List.of(configTopic, eventTopic)).all().get();
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
                .withName(ZILLABASE_KAFKA_VOLUME_NAME)
                .withLabels(Map.of(VOLUME_LABEL, PROJECT_NAME))
                .exec();

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withRestartPolicy(unlessStoppedRestart())
                    .withBinds(new Bind(ZILLABASE_KAFKA_VOLUME_NAME, new Volume(volume))))
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
            super(config, "apicurio", "apicurio/apicurio-registry:%s".formatted(config.registry.apicurio.tag));
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
                    "QUARKUS_HTTP_CORS_ORIGINS=*",
                    "APICURIO_STORAGE_KIND=kafkasql",
                    "APICURIO_KAFKASQL_BOOTSTRAP_SERVERS=%s".formatted(config.kafka.bootstrapUrl))
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

    private static final class CreatePostgresFactory extends CreateContainerFactory
    {
        CreatePostgresFactory(
            ZillabaseConfig config)
        {
            super(config, "postgres", "postgres:15-alpine");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            client.createVolumeCmd()
                .withName(ZILLABASE_POSTGRES_VOLUME_NAME)
                .withLabels(Map.of(VOLUME_LABEL, PROJECT_NAME))
                .exec();

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withRestartPolicy(unlessStoppedRestart())
                    .withBinds(new Bind(
                        ZILLABASE_POSTGRES_VOLUME_NAME, new Volume("/var/lib/postgresql/data"))))
                .withEnv(
                    "POSTGRES_USER=postgres",
                    "POSTGRES_DB=metadata",
                    "POSTGRES_HOST_AUTH_METHOD=trust",
                    "POSTGRES_INITDB_ARGS=--encoding=UTF-8 --lc-collate=C --lc-ctype=C")
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(2L))
                    .withTimeout(SECONDS.toNanos(5L))
                    .withRetries(5)
                    .withTest(List.of("CMD-SHELL", "pg_isready -U postgres")));
        }
    }


    private static final class CreateMinioFactory extends CreateContainerFactory
    {
        CreateMinioFactory(
            ZillabaseConfig config)
        {
            super(config, "minio", "quay.io/minio/minio:RELEASE.2024-11-07T00-52-20Z");
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            client.createVolumeCmd()
                .withName(ZILLABASE_MINIO_VOLUME_NAME)
                .withLabels(Map.of(VOLUME_LABEL, PROJECT_NAME))
                .exec();

            return client
                .createContainerCmd(image)
                .withLabels(project)
                .withName(name)
                .withHostName(hostname)
                .withHostConfig(HostConfig.newHostConfig()
                    .withNetworkMode(network)
                    .withRestartPolicy(unlessStoppedRestart())
                    .withBinds(new Bind(ZILLABASE_MINIO_VOLUME_NAME, new Volume("/data"))))
                .withTty(true)
                .withCmd("server", "--address", "0.0.0.0:9301", "/data")
                .withEnv("MINIO_ROOT_PASSWORD=hummockadmin",
                    "MINIO_ROOT_USER=hummockadmin")
                .withEntrypoint(
                    "/bin/sh", "-c",
                    String.join(" && ",
                        "set -e",
                        "mkdir -p /data/hummock001",
                        "/usr/bin/docker-entrypoint.sh \"$0\" \"$@\""))
                .withHealthcheck(new HealthCheck()
                    .withInterval(SECONDS.toNanos(1L))
                    .withTimeout(SECONDS.toNanos(5L))
                    .withRetries(5)
                    .withTest(List.of("CMD", "bash", "-c", "echo -n '' > /dev/tcp/127.0.0.1/9301")));
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
                .withEnv("RW_STANDALONE_META_OPTS=" +
                        "--listen-addr 0.0.0.0:5690 " +
                        "--advertise-addr 0.0.0.0:5690 " +
                        "--dashboard-host 0.0.0.0:5691 " +
                        "--backend sql " +
                        "--sql-endpoint postgres://postgres:@postgres.zillabase.dev:5432/metadata " +
                        "--state-store hummock+minio://hummockadmin:hummockadmin@minio.zillabase.dev:9301/hummock001 " +
                        "--data-directory hummock_001",
                    "RW_STANDALONE_COMPUTE_OPTS=" +
                        "--listen-addr 0.0.0.0:5688 " +
                        "--advertise-addr 0.0.0.0:5688 " +
                        "--meta-address http://0.0.0.0:5690 ",
                    "RW_STANDALONE_FRONTEND_OPTS=" +
                        "--listen-addr 0.0.0.0:4566 " +
                        "--advertise-addr 0.0.0.0:4566 " +
                        "--health-check-listener-addr 0.0.0.0:6786 " +
                        "--meta-addr http://0.0.0.0:5690 ",
                    "RW_STANDALONE_COMPACTOR_OPTS=" +
                        "--listen-addr 0.0.0.0:6660 " +
                        "--advertise-addr 0.0.0.0:6660 " +
                        "--meta-address http://0.0.0.0:5690"
                )
                .withCmd("standalone")
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

    private static final class CreateApiGenFactory extends CreateContainerFactory
    {
        CreateApiGenFactory(
            ZillabaseConfig config)
        {
            super(config, "api-gen", "ghcr.io/aklivity/zillabase/api-gen:%s".formatted(config.apiGen.tag));
        }

        @Override
        CreateContainerCmd createContainer(
            DockerClient client)
        {
            List<String> envVars = Arrays.asList(
                "ADMIN_HTTP_URL=http://admin.zillabase.dev:%d".formatted(DEFAULT_ADMIN_HTTP_PORT),
                "KAFKA_BOOTSTRAP_SERVERS=%s".formatted(config.kafka.bootstrapUrl),
                "KARAPACE_URL=%s".formatted(config.registry.karapace.url),
                "APICURIO_REGISTRY_URL=%s".formatted(config.registry.apicurio.url),
                "KEYLOAK_JWT_SECRET=%s".formatted(config.keycloak.jwks),
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
                .withTty(true);
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
                "PYTHON_UDF_SERVER_HOST=%s".formatted("udf-server-python.zillabase.dev"),
                "PYTHON_UDF_SERVER_PORT=%d".formatted(5000),
                "JAVA_UDF_SERVER_HOST=%s".formatted("udf-server-java.zillabase.dev"),
                "JAVA_UDF_SERVER_PORT=%d".formatted(5001),
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
            envVars.add("CLASSPATH=service-udf-java.jar:/opt/udf/lib/*");

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

            client.createVolumeCmd()
                .withName(ZILLABASE_UDF_PYTHON_VOLUME_NAME)
                .withLabels(Map.of(VOLUME_LABEL, PROJECT_NAME))
                .exec();
            binds.add(new Bind(ZILLABASE_UDF_PYTHON_VOLUME_NAME, new Volume("/usr/local/lib")));

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
                    .withRetries(60)
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
            ZillabaseRisingWaveConfig config,
            String user)
        {
            this.config = config;

            Properties props = new Properties();
            props.setProperty("user", user);
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
                process(sql.getFileName().toString(), content);
            }
        }

        void process(
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
