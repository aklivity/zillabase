package io.aklivity.zillabase.service.api.gen.internal.service;

import static io.aklivity.zillabase.service.api.gen.config.ZillabaseAdminConfig.DEFAULT_ADMIN_HTTP_PORT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiSpecRegisterResponse;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;

public abstract class AsyncapiService
{
    @Value("${DEFAULT_ADMIN_HTTP_PORT:7184}")
    private int defaultAdminHttpPort;

    @Value("${DEFAULT_KARAPACE_URL:http://karapace.zillabase.dev:8081}")
    private String defaultKarapaceUrl;


    protected static final Pattern TOPIC_PATTERN = Pattern.compile("(^|-|_)(.)");
    protected static final Pattern EXPRESSION_PATTERN =
        Pattern.compile("\\$\\{\\{\\s*([^\\s\\}]*)\\.([^\\s\\}]*)\\s*\\}\\}");
    protected static final Pattern PROTO_MESSAGE_PATTERN = Pattern.compile("message\\s+\\w+\\s*\\{[^}]*\\}",
        Pattern.DOTALL);
    protected static final String KAFKA_ASYNCAPI_ARTIFACT_ID = "kafka-asyncapi";
    protected static final String HTTP_ASYNCAPI_ARTIFACT_ID = "http-asyncapi";

    protected final Matcher matcher = TOPIC_PATTERN.matcher("");
    protected final Matcher envMatcher = EXPRESSION_PATTERN.matcher("");
    protected final Matcher protoMatcher = PROTO_MESSAGE_PATTERN.matcher("");
    protected final List<String> operations = new ArrayList<>();
    protected final List<KafkaTopicSchemaRecord> records = new ArrayList<>();

    final HttpClient client = HttpClient.newHttpClient();

    protected int registerAsyncApiSpec(
        String id,
        String spec)
    {
        try
        {
            File tempFile = File.createTempFile("zillabase-asyncapi-spec", ".tmp");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
            {
                writer.write(spec);
            }

            HttpRequest httpRequest = HttpRequest
                .newBuilder(toURI("http://localhost:%d".formatted(defaultAdminHttpPort),
                    "/v1/asyncapis"))
                .header("Content-Type", "application/vnd.aai.asyncapi+yaml")
                .header("X-Registry-ArtifactId", id)
                .POST(HttpRequest.BodyPublishers.ofString(spec)).build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            String response = httpResponse.statusCode() == 200 ? httpResponse.body() : null;

            if (response != null)
            {
                Jsonb jsonb = JsonbBuilder.newBuilder().build();
                AsyncapiSpecRegisterResponse register = jsonb.fromJson(response, AsyncapiSpecRegisterResponse.class);
                System.out.println("Registered AsyncAPI spec: %s".formatted(register.id));
            }
            else
            {
                System.out.println("Error registering AsyncAPI spec");
            }

            tempFile.delete();
        }
        catch (IOException | InterruptedException ex)
        {
            ex.printStackTrace(System.err);
        }
    }

    protected String resolveType(
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
                    type = switch (schemaType)
                    {
                        case "record", "enum", "fixed" -> "avro";
                        default -> "json";
                    };
                }
            }
        }
        catch (Exception ex)
        {
            System.err.format("Failed to parse schema type: %s:\n", ex.getMessage());
        }
        return type;
    }

    protected String resolveSchema(
        String subject)
    {
        String responseBody;
        try
        {
            HttpRequest httpRequest = HttpRequest
                .newBuilder(toURI(defaultKarapaceUrl,
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
}
