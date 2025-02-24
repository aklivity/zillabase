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
package io.aklivity.zillabase.service.api.gen.internal.generator;

import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.GET;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.POST;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.PUT;
import static io.aklivity.zillabase.service.api.gen.internal.helper.KafkaTopicSchemaHelper.toCamelCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationBinding;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelBinding;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelTopicCleanupPolicy;
import com.asyncapi.schemas.asyncapi.Reference;
import com.asyncapi.schemas.asyncapi.multiformat.AvroFormatSchema;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuth2SecurityScheme;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuthFlows;
import com.asyncapi.schemas.avro.v1._9_0.AvroSchema;
import com.asyncapi.schemas.avro.v1._9_0.AvroSchemaRecord;
import com.asyncapi.schemas.avro.v1._9_0.AvroSchemaRecordField;
import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.channel.Parameter;
import com.asyncapi.v3._0_0.model.channel.message.Message;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.operation.Operation;
import com.asyncapi.v3._0_0.model.operation.OperationAction;
import com.asyncapi.v3._0_0.model.server.Server;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiKafkaFilter;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiSpec;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiSpecBuilder;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaHttpOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaSseKafkaOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaSseOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.helper.KafkaTopicSchemaHelper;

@Component
public class HttpAsyncApiGenerator extends AsyncApiGenerator
{
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(\\w+)\\.(\\w+)");
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("ref=([^)]*)");
    private final Matcher matcher = TOPIC_PATTERN.matcher("");
    private final Matcher referenceMatcher = REFERENCE_PATTERN.matcher("");
    private final KafkaTopicSchemaHelper kafkaHelper;
    private final List<String> scopes;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpAsyncApiGenerator(
        KafkaTopicSchemaHelper kafkaHelper)
    {
        this.kafkaHelper = kafkaHelper;
        this.scopes = new ArrayList<>();
    }

    public String generate(
        String kafkaSpec) throws Exception
    {
        AsyncAPI kafkaApi = deserializeAsyncapi(kafkaSpec);
        String httpSpec = null;

        if (kafkaApi != null)
        {
            AsyncapiSpecBuilder<AsyncapiSpec> builder = AsyncapiSpec.builder()
                .inject(spec -> spec.asyncapi("3.0.0"))
                .inject(spec -> injectInfo(spec, "REST APIs"))
                .inject(this::injectServers)
                .inject(spec -> injectChannels(spec, kafkaApi))
                .inject(spec -> injectOperations(spec, kafkaApi))
                .inject(spec -> injectComponents(spec, kafkaApi));

            AsyncapiSpec spec = builder.build();
            httpSpec = buildYaml(spec);
        }

        return httpSpec;
    }

    private <C> AsyncapiSpecBuilder<C> injectServers(
        AsyncapiSpecBuilder<C> builder)
    {
        Reference securityRef = new Reference("#/components/securitySchemes/httpOauth");
        Server server = Server.builder()
            .host("localhost:8080")
            .protocol("http")
            .security(List.of(securityRef))
            .build();

        return builder.servers(Map.of("http", server));
    }

    private <C> AsyncapiSpecBuilder<C> injectChannels(
        AsyncapiSpecBuilder<C> builder,
        AsyncAPI kafkaApi)
    {
        if (kafkaApi.getChannels() != null)
        {
            kafkaApi.getChannels().forEach((k, v) ->
            {
                if (!k.endsWith("_replies"))
                {
                    injectChannels(builder, k);
                }
            });
        }

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectComponents(
        AsyncapiSpecBuilder<C> builder,
        AsyncAPI kafkaApi)
    {
        Components.ComponentsBuilder componentsBuilder = Components.builder();

        Components components = kafkaApi.getComponents();
        if (components != null)
        {
            injectSchemas(componentsBuilder, components.getSchemas());
            injectMessages(componentsBuilder, components.getMessages());
        }

        injectOauthSecurity(componentsBuilder);

        builder.components(componentsBuilder.build());

        return builder;
    }

    private void injectSchemas(
        Components.ComponentsBuilder builder,
        Map<String, Object> schemas)
    {
        builder.schemas(schemas.entrySet().stream()
            .flatMap(entry ->
            {
                String name = entry.getKey();
                ObjectNode schema = convertToAsyncapiSchema((AvroFormatSchema) entry.getValue());

                ObjectNode newSchema = convertToAsyncapiSchemas(schema);
                String newName = name.replace("-value", "-values");

                return Stream.of(
                    Map.entry(name, schema),
                    Map.entry(newName, newSchema)
                );
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    private ObjectNode convertToAsyncapiSchema(
        AvroFormatSchema avroFormatSchema)
    {
        ObjectNode jsonSchema = objectMapper.createObjectNode();
        AvroSchemaRecord schema = (AvroSchemaRecord) avroFormatSchema.getSchema();

        if (schema.getType().equals("record"))
        {
            jsonSchema.put("type", "object");

            ObjectNode propertiesNode = objectMapper.createObjectNode();
            ArrayNode requiredNode = objectMapper.createArrayNode();

            for (AvroSchemaRecordField field : schema.getFields())
            {
                String fieldName = field.getName();
                propertiesNode.set(fieldName, mapAvroTypeToJsonSchema(field.getType()));
                requiredNode.add(fieldName);
            }

            jsonSchema.set("properties", propertiesNode);
            jsonSchema.set("required", requiredNode);
        }

        return jsonSchema;
    }

    private ObjectNode convertToAsyncapiSchemas(
        ObjectNode schema)
    {
        ObjectNode schemas = objectMapper.createObjectNode();
        schemas.put("type", "array");
        schemas.set("items", schema);

        return schemas;
    }

    public JsonNode mapAvroTypeToJsonSchema(
        Object avroType)
    {
        if (avroType instanceof String)
        {
            return handlePrimitiveType((String) avroType);
        }
        if (avroType instanceof AvroSchema)
        {
            return handlePrimitiveType(((AvroSchema) avroType).getType());
        }
        else if (avroType instanceof Map)
        {
            return handleComplexType((Map<String, Object>) avroType);
        }
        else if (avroType instanceof List)
        {
            return handleUnionType((List<Object>) avroType);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported Avro type: " + avroType.getClass().getName());
        }
    }

    private JsonNode handlePrimitiveType(
        String type)
    {
        ObjectNode jsonTypeNode = objectMapper.createObjectNode();
        switch (type)
        {
        case "int":
            jsonTypeNode.put("type", "integer");
            break;
        case "long":
            jsonTypeNode.put("type", "integer");
            jsonTypeNode.put("format", "int64");
            break;
        case "float":
        case "double":
            jsonTypeNode.put("type", "number");
            break;
        case "boolean":
            jsonTypeNode.put("type", "boolean");
            break;
        case "string":
            jsonTypeNode.put("type", "string");
            break;
        case "null":
            jsonTypeNode.put("type", "null");
            break;
        default:
            jsonTypeNode.put("type", "string");
        }

        return jsonTypeNode;
    }

    private JsonNode handleComplexType(
        Map<String, Object> type)
    {
        ObjectNode jsonTypeNode = objectMapper.createObjectNode();
        String typeName = (String) type.get("type");

        switch (typeName)
        {
        case "record":
            jsonTypeNode.put("type", "object");
            ObjectNode properties = jsonTypeNode.putObject("properties");
            List<Map<String, Object>> fields = (List<Map<String, Object>>) type.get("fields");
            for (Map<String, Object> field : fields)
            {
                String fieldName = (String) field.get("name");
                Object fieldType = field.get("type");
                properties.set(fieldName, mapAvroTypeToJsonSchema(fieldType));
            }
            break;
        case "enum":
            jsonTypeNode.put("type", "string");
            ArrayNode enumValues = jsonTypeNode.putArray("enum");
            List<String> symbols = (List<String>) type.get("symbols");
            symbols.forEach(enumValues::add);
            break;
        case "array":
            jsonTypeNode.put("type", "array");
            jsonTypeNode.set("items", mapAvroTypeToJsonSchema(type.get("items")));
            break;
        case "map":
            jsonTypeNode.put("type", "object");
            jsonTypeNode.set("additionalProperties", mapAvroTypeToJsonSchema(type.get("values")));
            break;
        default:
            jsonTypeNode.put("type", "string");
        }
        return jsonTypeNode;
    }

    private JsonNode handleUnionType(
        List<Object> types)
    {
        JsonNode node;

        if (types.size() == 1)
        {
            node = mapAvroTypeToJsonSchema(types.get(0));
        }
        else
        {
            ArrayNode oneOf = objectMapper.createArrayNode();
            for (Object type : types)
            {
                oneOf.add(mapAvroTypeToJsonSchema(type));
            }
            ObjectNode unionNode = objectMapper.createObjectNode();
            unionNode.set("oneOf", oneOf);
            node = unionNode;
        }

        return node;
    }

    private void injectMessages(
        Components.ComponentsBuilder builder,
        Map<String, Object> messages)
    {
        builder.messages(messages.entrySet().stream()
            .flatMap(entry ->
            {
                String originalName = entry.getKey();
                Message originalMessage =  (Message) entry.getValue();

                Map.Entry<String, Message> originalEntry = Map.entry(originalName, originalMessage);

                String newName = originalName.replace("Message", "Messages");
                String reference = originalEntry.getValue().getPayload().toString();
                referenceMatcher.reset(reference);
                Reference newReference = referenceMatcher.find()
                    ? new Reference(referenceMatcher.group(1).replace("-value", "-values"))
                    : null;

                Message newMessage = Message.builder()
                    .name(newName)
                    .contentType(originalMessage.getContentType())
                    .payload(newReference)
                    .build();

                Map.Entry<String, Message> newEntry = Map.entry(newName, newMessage);

                return Stream.of(originalEntry, newEntry);
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    private void injectOauthSecurity(
        Components.ComponentsBuilder builder)
    {
        if (!scopes.isEmpty())
        {
            OAuth2SecurityScheme scheme = OAuth2SecurityScheme.oauth2Builder()
                .flows(new OAuthFlows())
                .scopes(scopes)
                .build();
            Map<String, Object> security = Map.of("httpOauth", scheme);
            builder.securitySchemes(security);
        }
    }

    private <C> AsyncapiSpecBuilder<C> injectChannels(
        AsyncapiSpecBuilder<C> builder,
        String channelName)
    {
        String name = matcher.reset(channelName).replaceFirst(m -> m.group(2));
        String label = toCamelCase(name);

        addReadWriteScopes(name);

        builder.inject(spec -> injectItemsChannel(spec, label, name))
            .inject(spec -> injectItemChannel(spec, label, name))
            .inject(spec -> injectStreamChannel(spec, label, name))
            .inject(spec -> injectIdentityChannel(spec, label, name));

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectItemsChannel(
        AsyncapiSpecBuilder<C> builder,
        String label,
        String name)
    {
        Reference message = new Reference("#/components/messages/%sMessage".formatted(label));
        Reference messages = new Reference("#/components/messages/%sMessages".formatted(label));
        String messageRef = "%sMessage".formatted(label);
        String messagesRef = "%sMessages".formatted(label);

        builder.addChannel(name, Channel.builder()
            .address("/%s".formatted(name))
            .messages(Map.of(
                messageRef, message,
                messagesRef, messages
            ))
            .build());

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectItemChannel(
        AsyncapiSpecBuilder<C> builder,
        String label,
        String name)
    {
        Reference message = new Reference("#/components/messages/%sMessage".formatted(label));
        String messageRef = "%sMessage".formatted(label);

        builder.addChannel("%s-item".formatted(name), Channel.builder()
            .address("/" + name + "/{id}")
            .parameters(Map.of("id", Parameter.builder()
                .description("Id of the item.")
                .build()))
            .messages(Map.of(messageRef, message))
            .build());

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectStreamChannel(
        AsyncapiSpecBuilder<C> builder,
        String label,
        String name)
    {
        Reference message = new Reference("#/components/messages/%sMessage".formatted(label));
        String messageRef = "%sMessage".formatted(label);

        builder.addChannel("%s-stream".formatted(name), Channel.builder()
            .address("/%s-stream".formatted(name))
            .messages(Map.of(messageRef, message))
            .build());

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectIdentityChannel(
        AsyncapiSpecBuilder<C> builder,
        String label,
        String name)
    {
        Reference message = new Reference("#/components/messages/%sMessage".formatted(label));
        String messageRef = "%sMessage".formatted(label);

        builder.addChannel("%s-stream-identity".formatted(name), Channel.builder()
            .address("/%s-stream-identity".formatted(name))
            .messages(Map.of(messageRef, message))
            .build());


        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectOperations(
        AsyncapiSpecBuilder<C> builder,
        AsyncAPI kafkaApi)
    {
        Map<String, Object> channels = kafkaApi.getChannels();
        channels.entrySet().stream()
            .filter(e -> !e.getKey().endsWith("_replies"))
            .forEach(entry ->
            {
                Channel channel = (Channel) entry.getValue();

                String channelName = entry.getKey();
                boolean compact = isTopicCompact(channel);
                String name = matcher.reset(channelName).replaceFirst(m -> m.group(2));
                String label = toCamelCase(name);

                injectHttpOperations(builder, name, label, compact);
            });

        return builder;
    }

    private void addReadWriteScopes(
        String label)
    {
        String base = label;
        scopes.add(base + ":read");
        scopes.add(base + ":write");
    }

    private boolean isTopicCompact(
        Channel channel)
    {
        boolean compact = false;
        Map<String, Object> bindings = channel.getBindings();

        KafkaChannelBinding kafka = bindings != null
            ? (KafkaChannelBinding) bindings.get("kafka")
            : null;

        if (kafka != null && kafka.getTopicConfiguration() != null)
        {
            List<KafkaChannelTopicCleanupPolicy> cleanupPolicy = kafka
                .getTopicConfiguration()
                .getCleanupPolicy();

            compact = cleanupPolicy != null && cleanupPolicy.stream()
                .anyMatch(p -> "compact".equalsIgnoreCase(p.name()));
        }

        return compact;
    }

    private <C> AsyncapiSpecBuilder<C> injectHttpOperations(
        AsyncapiSpecBuilder<C> builder,
        String name,
        String label,
        boolean compact)
    {
        String identity = resolveIdentityField(label);

        return builder
            .inject(spec -> injectPostOperation(spec, name, label, compact))
            .inject(spec -> injectPutOperation(spec, name, label, compact))
            .inject(spec -> injectGetOperations(spec, name, label, identity))
            .inject(spec -> injectSseOperations(spec, name, label, identity));
    }

    private String resolveIdentityField(
        String label)
    {
        String identity = null;
        try
        {
            identity = kafkaHelper.resolve()
                .stream()
                .filter(r -> label.equals(r.label))
                .findFirst()
                .map(r -> "protobuf".equalsIgnoreCase(r.type)
                    ? kafkaHelper.findIdentityFieldFromProtobuf(r.schema)
                    : kafkaHelper.findIdentityField(r.schema))
                .orElse(null);
        }
        catch (Exception e)
        {
            // ignore
        }

        return identity;
    }

    private <C> AsyncapiSpecBuilder<C> injectPostOperation(
        AsyncapiSpecBuilder<C> builder,
        String name,
        String label,
        boolean compact)
    {
        Operation op = Operation.builder()
            .action(OperationAction.SEND)
            .channel(new Reference("#/channels/%s".formatted(name)))
            .messages(Collections.singletonList(
                new Reference("#/channels/%s/messages/%sMessage".formatted(name, label))))
            .build();

        HTTPOperationBinding http = HTTPOperationBinding.builder()
            .method(POST)
            .build();

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("http", http);

        ZillaHttpOperationBinding zilla = new ZillaHttpOperationBinding(POST,
            Map.of("zilla:identity", "{identity}"), null);
        bindings.put("x-zilla-http-kafka", zilla);

        op.setSecurity(List.of(new Reference("#/components/securitySchemes/httpOauth")));
        op.setBindings(bindings);
        String opName = compact ? "do%sCreate".formatted(label) : "do%s".formatted(label);

        builder.addOperation(opName, op);

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectPutOperation(
        AsyncapiSpecBuilder<C> builder,
        String name,
        String label,
        boolean compact)
    {
        if (compact)
        {
            Operation op = Operation.builder()
                .action(OperationAction.SEND)
                .channel(new Reference("#/channels/%s-item".formatted(name)))
                .messages(Collections.singletonList(
                    new Reference("#/channels/%s-item/messages/%sMessage".formatted(name, label))))
                .build();

            HTTPOperationBinding put = HTTPOperationBinding.builder()
                .method(PUT)
                .build();
            Map<String, Object> bindings = new HashMap<>();
            bindings.put("http", put);

            ZillaHttpOperationBinding zilla = new ZillaHttpOperationBinding(PUT,
                Map.of("zilla:identity", "{identity}"), null);
            bindings.put("x-zilla-http-kafka", zilla);
            op.setSecurity(List.of(new Reference("#/components/securitySchemes/httpOauth")));
            op.setBindings(bindings);

            String opName = "do%sUpdate".formatted(label);

            builder.addOperation(opName, op);
        }

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectGetOperations(
        AsyncapiSpecBuilder<C> builder,
        String name,
        String label,
        String identity)
    {
        HTTPOperationBinding get = HTTPOperationBinding.builder()
            .method(GET)
            .build();

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("http", get);

        if (identity != null)
        {
            AsyncapiKafkaFilter f = new AsyncapiKafkaFilter();
            f.headers = Map.of("identity", "{identity}");
            ZillaHttpOperationBinding zh = new ZillaHttpOperationBinding(GET, null, List.of(f));
            bindings.put("x-zilla-http-kafka", zh);
        }

        builder
            .inject(spec -> injectGetItemOperation(spec, bindings, name, label))
            .inject(spec -> injectGetOperation(spec, bindings, name, label));

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectGetOperation(
        AsyncapiSpecBuilder<C> builder,
        Map<String, Object> bindings,
        String name,
        String label)
    {
        Operation allGetOp = Operation.builder()
            .action(OperationAction.RECEIVE)
            .channel(new Reference("#/channels/%s".formatted(name)))
            .messages(Collections.singletonList(
                new Reference("#/channels/%s/messages/%sMessages".formatted(name, label))))
            .bindings(bindings)
            .security(List.of(new Reference("#/components/securitySchemes/httpOauth")))
            .build();

        String allOpName = "on%sGet".formatted(label);
        builder.addOperation(allOpName, allGetOp);

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectGetItemOperation(
        AsyncapiSpecBuilder<C> builder,
        Map<String, Object> bindings,
        String name,
        String label)
    {
        Operation item = Operation.builder()
            .action(OperationAction.RECEIVE)
            .channel(new Reference("#/channels/%s-item".formatted(name)))
            .messages(Collections.singletonList(
                new Reference("#/channels/%s-item/messages/%sMessage".formatted(name, label))))
            .bindings(bindings)
            .security(List.of(new Reference("#/components/securitySchemes/httpOauth")))
            .build();

        String itemOpName = "on%sGetItem".formatted(label);
        builder.addOperation(itemOpName, item);

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectSseOperations(
        AsyncapiSpecBuilder<C> builder,
        String name,
        String label,
        String identity)
    {
        List<Object> security = List.of(new Reference("#/components/securitySchemes/httpOauth"));

        builder
            .inject(spec -> injectSseStreamOperations(spec, name, label, security, identity))
            .inject(spec -> injectSseIdentityOperations(spec, name, label, security));

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectSseIdentityOperations(
        AsyncapiSpecBuilder<C> builder,
        String name,
        String label,
        List<Object> security)
    {
        ZillaSseOperationBinding sse = new ZillaSseOperationBinding();

        Map<String, Object> bindings = new HashMap<>();
        bindings.put("x-zilla-sse", sse);

        AsyncapiKafkaFilter filter = new AsyncapiKafkaFilter();
        filter.key = "{identity}";
        bindings.put("x-zilla-sse-kafka", new ZillaSseKafkaOperationBinding(List.of(filter)));

        builder.addOperation(
            "on%sReadItem".formatted(label),
            Operation.builder()
                .action(OperationAction.RECEIVE)
                .channel(new Reference("#/channels/%s-stream-identity".formatted(name)))
                .messages(Collections.singletonList(
                    new Reference("#/channels/%s-stream-identity/messages/%sMessage".formatted(name, label))))
                .bindings(bindings)
                .security(security)
                .build());

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectSseStreamOperations(
        AsyncapiSpecBuilder<C> builder,
        String name,
        String label,
        List<Object> security,
        String identity)
    {
        Map<String, Object> bindings = new HashMap<>();

        ZillaSseOperationBinding sse = new ZillaSseOperationBinding();
        bindings.put("x-zilla-sse", sse);

        if (identity != null)
        {
            AsyncapiKafkaFilter filter = new AsyncapiKafkaFilter();
            filter.headers = Map.of("identity", "{identity}");
            bindings.put("x-zilla-sse-kafka", new ZillaSseKafkaOperationBinding(List.of(filter)));
        }

        builder.addOperation(
            "on%sRead".formatted(label),
            Operation.builder()
                .action(OperationAction.RECEIVE)
                .channel(new Reference("#/channels/%s-stream".formatted(name)))
                .messages(Collections.singletonList(
                    new Reference("#/channels/%s-stream/messages/%sMessage".formatted(name, label))))
                .bindings(bindings)
                .security(security)
                .build());

        return builder;
    }

    private AsyncAPI deserializeAsyncapi(
        String yaml)
    {
        AsyncAPI spec = null;
        try
        {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            spec = yamlMapper.readValue(yaml, AsyncAPI.class);
        }
        catch (JsonProcessingException e)
        {
            System.out.println("Failed to deserialize asyncapi: " + yaml);
        }

        return spec;
    }
}
