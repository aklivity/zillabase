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

import static io.aklivity.zillabase.cli.config.ZillabaseConfigServerConfig.ZILLABASE_API_GEN_EVENTS_KAFKA_TOPIC;
import static io.aklivity.zillabase.cli.config.ZillabaseConfigServerConfig.ZILLABASE_CONFIG_KAFKA_TOPIC;
import static io.aklivity.zillabase.cli.config.ZillabaseKafkaConfig.DEFAULT_KAFKA_BOOTSTRAP_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseKarapaceConfig.DEFAULT_CLIENT_KARAPACE_URL;
import static io.aklivity.zillabase.cli.config.ZillabaseKarapaceConfig.DEFAULT_KARAPACE_URL;
import static java.net.http.HttpClient.Version.HTTP_1_1;

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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
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
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.ResponseItem;
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseAdminConfig;
import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakClientConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakUserConfig;
import io.aklivity.zillabase.cli.config.ZillabaseRisingWaveConfig;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseDockerCommand;
import io.aklivity.zillabase.cli.internal.kafka.KafkaBootstrapRecords;
import io.aklivity.zillabase.cli.internal.kafka.KafkaTopicRecord;
import io.aklivity.zillabase.cli.internal.kafka.KafkaTopicSchema;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationApplier;
import io.aklivity.zillabase.cli.internal.migrations.ZillabaseMigrationService;
import io.aklivity.zillabase.cli.internal.migrations.model.ZillabaseMigrationFile;

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

        createConfigServerKafkaTopic(config);

        seedKafkaAndRegistry(config);

        initializeKeycloakService(config);

        processInitSql(config);

        processSql(config);

        processSystemSql(config);

        printExposedEndpoints(config);

    }

    private void printExposedEndpoints(
        ZillabaseConfig config)
    {
        int studioPort = config.studio.port;
        int pgsqlPort = config.admin.pgsqlPort;

        String studioUrl = "Studio UI: http://localhost:%d".formatted(studioPort);
        String psqlUrl = "Psql: localhost:%d".formatted(pgsqlPort);

        int maxLength = Math.max(studioUrl.length(), psqlUrl.length());
        String border = "#".repeat(maxLength + 4);

        System.out.println(border);
        System.out.printf("# %-" + maxLength + "s #\n", studioUrl);
        System.out.printf("# %-" + maxLength + "s #\n", psqlUrl);
        System.out.println(border);
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
                    sql VARCHAR
                );
                CREATE TABLE zb_catalog.ztables(
                    name VARCHAR PRIMARY KEY,
                    sql VARCHAR
                );
                CREATE TABLE zb_catalog.zfunctions(
                    name VARCHAR PRIMARY KEY,
                    sql VARCHAR
                );
                CREATE TABLE zb_catalog.schema_version(
                    version VARCHAR PRIMARY KEY,
                    description VARCHAR,
                    script_name VARCHAR,
                    checksum VARCHAR,
                    applied_on TIMESTAMP
                );
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
            final int port = ZillabaseAdminConfig.DEFAULT_ADMIN_PGSQL_PORT;
            final String db = config.risingwave.db;

            ZillabaseMigrationService migrationService = new ZillabaseMigrationService(port, db);
            List<ZillabaseMigrationFile> unappliedFiles = migrationService.unappliedFiles();

            ZillabaseMigrationApplier applier = new ZillabaseMigrationApplier(port, db);
            applier.apply(unappliedFiles);

            pgsql.process(seedSqlPath);
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
