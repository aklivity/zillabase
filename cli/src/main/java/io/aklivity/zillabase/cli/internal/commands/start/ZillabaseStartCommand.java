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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
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
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonSchemaReader;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.dockerjava.api.DockerClient;
import com.github.rvesse.airline.annotations.Command;

import io.aklivity.zillabase.cli.config.ZillabaseConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakClientConfig;
import io.aklivity.zillabase.cli.config.ZillabaseKeycloakUserConfig;
import io.aklivity.zillabase.cli.internal.commands.ZillabaseDockerCommand;

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

    public static final String PROJECT_NAME = ZILLABASE_PATH.toAbsolutePath().getParent().getFileName().toString();
    public static final String VOLUME_LABEL = "io.aklivity.zillabase.cli.project";

    private final Matcher envMatcher = EXPRESSION_PATTERN.matcher("");

    public String kafkaSeedFilePath = "zillabase/seed-kafka.yaml";

    @Override
    protected void invoke(
        DockerClient client)
    {
        final ZillabaseConfig config = readZillabaseConfig();

        initializeKeycloakService(config);

        printExposedEndpoints(config);
    }

    private void printExposedEndpoints(
        ZillabaseConfig config)
    {
        int studioPort = config.studio.port;

        String studioUrl = "Studio UI: http://localhost:%d".formatted(studioPort);

        int maxLength = studioUrl.length();
        String border = "#".repeat(maxLength + 4);

        System.out.println(border);
        System.out.printf("# %-" + maxLength + "s #\n", studioUrl);
        System.out.println(border);
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

    private URI toURI(
        String baseUrl,
        String path)
    {
        return URI.create(baseUrl).resolve(path);
    }
}
