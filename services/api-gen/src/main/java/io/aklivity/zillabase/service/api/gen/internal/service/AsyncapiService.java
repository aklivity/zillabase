package io.aklivity.zillabase.service.api.gen.internal.service;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiSpecRegisterResponse;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;

public abstract class AsyncapiService
{
    protected static final Pattern TOPIC_PATTERN = Pattern.compile("(^|-|_)(.)");
    protected static final Pattern EXPRESSION_PATTERN =
        Pattern.compile("\\$\\{\\{\\s*([^\\s\\}]*)\\.([^\\s\\}]*)\\s*\\}\\}");
    protected static final Pattern PROTO_MESSAGE_PATTERN = Pattern.compile("message\\s+\\w+\\s*\\{[^}]*\\}",
        Pattern.DOTALL);

    protected final Matcher matcher = TOPIC_PATTERN.matcher("");
    protected final Matcher envMatcher = EXPRESSION_PATTERN.matcher("");
    protected final Matcher protoMatcher = PROTO_MESSAGE_PATTERN.matcher("");
    protected final List<String> operations = new ArrayList<>();
    protected final List<KafkaTopicSchemaRecord> records = new ArrayList<>();

    protected final HttpClient client = HttpClient.newHttpClient();

    @Autowired
    protected ApiGenConfig config;

    protected String registerAsyncApiSpec(
        String id,
        String spec)
    {
        String newVersion = null;
        try
        {
            File tempFile = File.createTempFile("zillabase-asyncapi-spec", ".tmp");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile)))
            {
                writer.write(spec);
            }

            HttpRequest httpRequest = HttpRequest
                .newBuilder(toURI("http://localhost:%d".formatted(config.adminHttpPort()),
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
                newVersion = register.id;
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

        return newVersion;
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
                .newBuilder(toURI(config.karapaceUrl(),
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

    protected String buildAsyncApiSpec(
        Info info,
        Components components,
        Map<String, Object> channels,
        Map<String, Object> operations,
        Map<String, Object> servers) throws JsonProcessingException
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

        return  mapper.writeValueAsString(asyncAPI);
    }

    protected URI toURI(
        String baseUrl,
        String path)
    {
        return URI.create(baseUrl).resolve(path);
    }

    protected String extractIdentityFieldFromProtobufSchema(
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

    protected String extractIdentityFieldFromSchema(
        String schema) throws JsonProcessingException
    {
        AtomicReference<String> identity = new AtomicReference<>(null);
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

        return identity.get();
    }
}
