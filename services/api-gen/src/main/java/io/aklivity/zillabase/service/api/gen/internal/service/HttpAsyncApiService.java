package io.aklivity.zillabase.service.api.gen.internal.service;

import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.GET;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.POST;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.PUT;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationBinding;
import com.asyncapi.schemas.asyncapi.Reference;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuth2SecurityScheme;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuthFlows;
import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.channel.Parameter;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.asyncapi.v3._0_0.model.info.License;
import com.asyncapi.v3._0_0.model.operation.Operation;
import com.asyncapi.v3._0_0.model.operation.OperationAction;
import com.asyncapi.v3._0_0.model.server.Server;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Service;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiKafkaFilter;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaHttpOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaSseKafkaOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaSseOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventState;

@Service
public class HttpAsyncApiService
{
    public static final String HTTP_ASYNCAPI_ARTIFACT_ID = "http-asyncapi";

    private final AsyncapiSpecService asyncapiSpecService;

    public HttpAsyncApiService(
        AsyncapiSpecService asyncapiSpecService)
    {
        this.asyncapiSpecService = asyncapiSpecService;
    }

    public ApiGenEvent generate(
        ApiGenEvent event)
    {
        ApiGenEvent newEvent;

        try
        {
            //TODO: Fetch Kafka Spec
            String httpSpec = generateHttpAsyncApiSpecs(null);

            String specVersion = asyncapiSpecService.register(HTTP_ASYNCAPI_ARTIFACT_ID, httpSpec);

            newEvent = new ApiGenEvent(ApiGenEventState.HTTP_ASYNC_API_PUBLISHED, event.kafkaVersion(), specVersion);
        }
        catch (Exception ex)
        {
            System.err.println("Error building AsyncApi Spec");
            ex.printStackTrace(System.err);

            newEvent = new ApiGenEvent(ApiGenEventState.HTTP_ASYNC_API_PUBLISHED, event.kafkaVersion(), null);
        }

        return newEvent;

    }

    private String generateHttpAsyncApiSpecs(
        String kafkaSpec) throws JsonProcessingException
    {
        String spec = null;
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

        boolean secure = true;

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
            String channelName = channelJson.getKey();
            if (channelName.endsWith("_replies_sink"))
            {
                continue;
            }

            String name = matcher.reset(channelName).replaceFirst(match -> match.group(2));
            String label = name.toUpperCase();

            if (secure)
            {
                //TODO: add support for keycloak
                //String scope = label.toLowerCase();
                //config.keycloak.scopes.add("%s:read".formatted(scope));
                //config.keycloak.scopes.add("%s:write".formatted(scope));
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
                arraySchemaKey.replace("%s.".formatted(config.risingwaveDb()), ""));
            arraySchema.put("namespace", config.risingwaveDb());
            schemas.put(arraySchemaKey, arraySchema);
        }

        if (secure)
        {
            //TODO: add support for keycloak
            OAuth2SecurityScheme securityScheme = OAuth2SecurityScheme.oauth2Builder()
                .flows(new OAuthFlows())
                .scopes(Collections.emptyList())
                .build();
            components.setSecuritySchemes(Map.of(securitySchemaName, securityScheme));
        }

        components.setSchemas(schemas);
        components.setMessages(messages);

        spec = buildAsyncApiSpec(info, components, channels, operations, servers);

        return spec;
    }

    private void generateHttpOperations(
        Map<String, Object> operations,
        boolean secure,
        Reference security,
        String name,
        String label,
        String messageName,
        boolean compact) throws JsonProcessingException
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
}
