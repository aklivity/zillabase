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
package io.aklivity.zillabase.service.api.gen.internal.builder;

import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.GET;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.POST;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.PUT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationBinding;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelBinding;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelTopicCleanupPolicy;
import com.asyncapi.schemas.asyncapi.Reference;
import com.asyncapi.schemas.asyncapi.multiformat.AvroFormatSchema;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuth2SecurityScheme;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuthFlows;
import com.asyncapi.schemas.avro.v1._9_0.AvroSchemaRecord;
import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.channel.Parameter;
import com.asyncapi.v3._0_0.model.channel.message.Message;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.asyncapi.v3._0_0.model.info.License;
import com.asyncapi.v3._0_0.model.operation.Operation;
import com.asyncapi.v3._0_0.model.operation.OperationAction;
import com.asyncapi.v3._0_0.model.server.Server;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiKafkaFilter;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaHttpOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaSseKafkaOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaSseOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;

public class HttpAsyncApiBuilder
{
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(\\w+)\\.(\\w+)");
    private static final Pattern REFERENCE_PATTERN = Pattern.compile("ref=([^)]*)");
    private final Matcher matcher = TOPIC_PATTERN.matcher("");
    private final Matcher referenceMatcher = REFERENCE_PATTERN.matcher("");
    private final KafkaTopicSchemaHelper kafkaHelper;
    private final List<String> scopes;

    public HttpAsyncApiBuilder(
        KafkaTopicSchemaHelper kafkaHelper)
    {
        this.kafkaHelper = kafkaHelper;
        this.scopes = new ArrayList<>();
    }

    public String buildSpec(
        String kafkaSpec) throws Exception
    {
        AsyncAPI kafkaApi = deserializeAsyncapi(kafkaSpec);
        String httpSpec = null;

        if (kafkaApi != null)
        {
            AsyncapiSpecBuilder<AsyncapiSpec> builder = AsyncapiSpec.builder()
                .inject(spec -> spec.asyncapi("3.0.0"))
                .inject(this::injectInfo)
                .inject(this::injectServers)
                .inject(spec -> injectChannels(spec, kafkaApi))
                .inject(spec -> injectOperations(spec, kafkaApi))
                .inject(spec -> injectComponents(spec, kafkaApi));

            AsyncapiSpec spec = builder.build();
            httpSpec = buildYaml(spec);
        }

        return httpSpec;
    }

    private <C> AsyncapiSpecBuilder<C> injectInfo(
        AsyncapiSpecBuilder<C> builder)
    {
        Info info = Info.builder()
            .title("API Document for REST APIs")
            .version("1.0.0")
            .license(new License(
                "Aklivity Community License",
                "https://github.com/aklivity/zillabase/blob/develop/LICENSE"
            ))
            .build();
        return builder.info(info);
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
        Map<String, Object> channels = kafkaApi.getChannels();
        for (Map.Entry<String, Object> entry : channels.entrySet())
        {
            String channelName = entry.getKey();
            if (!channelName.endsWith("_replies"))
            {
                injectChannel(builder, channelName);
            }
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
        ObjectMapper mapper = new ObjectMapper();

        builder.schemas(schemas.entrySet().stream()
            .flatMap(entry -> {
                String originalName = entry.getKey();
                AvroFormatSchema originalSchema = (AvroFormatSchema) entry.getValue();

                // Convert schema into array schema format
                AvroFormatSchema newSchema = convertToAvroArraySchema(mapper, originalSchema);
                String newSchemaName = originalName.replace("-value", "-values");

                return Stream.of(
                    Map.entry(originalName, originalSchema),
                    Map.entry(newSchemaName, newSchema)
                );
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    private AvroFormatSchema convertToAvroArraySchema(
        ObjectMapper mapper,
        AvroFormatSchema originalSchema)
    {
        AvroSchemaRecord originalSchemaRecord = (AvroSchemaRecord) originalSchema.getSchema();

        ObjectNode recordNode = mapper.createObjectNode()
            .put("type", "record")
            .put("name", originalSchemaRecord.getName())
            .put("namespace", originalSchemaRecord.getNamespace());

        ArrayNode fieldsNode = mapper.createArrayNode();
        originalSchemaRecord.getFields().forEach(f ->
        {
            ObjectNode fieldNode = mapper.createObjectNode();
            fieldNode.put("name", f.getName());
            fieldNode.set("type", mapper.valueToTree(f.getType()));
            fieldsNode.add(fieldNode);
        });
        recordNode.set("fields", fieldsNode);

        return new AvroFormatSchema(
            mapper.createObjectNode()
                .put("type", "array")
                .set("items", recordNode)
        );
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

    private <C> AsyncapiSpecBuilder<C> injectChannel(
        AsyncapiSpecBuilder<C> builder,
        String channelName)
    {
        String name = matcher.reset(channelName).replaceFirst(m -> m.group(2));
        String label = name.toUpperCase();

        addReadWriteScopes(label);

        Reference message = new Reference("#/components/messages/%sMessage".formatted(label));
        Reference messages = new Reference("#/components/messages/%sMessages".formatted(label));

        String messageRef = "%sMessage".formatted(label);
        String messagesRef = "%sMessages".formatted(label);

        builder.addChannel(name, Channel.builder()
            .address("/" + name)
            .messages(Map.of(
                messageRef, message,
                messagesRef, messages
            ))
            .build());

        builder.addChannel(name + "-item", Channel.builder()
            .address("/" + name + "/{id}")
            .parameters(Map.of("id", Parameter.builder()
                .description("Id of the item.")
                .build()))
            .messages(Map.of(messageRef, message))
            .build());

        builder.addChannel(name + "-stream", Channel.builder()
            .address("/" + name + "-stream")
            .messages(Map.of(messageRef, message))
            .build());

        builder.addChannel(name + "-stream-identity", Channel.builder()
            .address("/" + name + "-stream-identity")
            .messages(Map.of(messageRef, message))
            .build());

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectOperations(
        AsyncapiSpecBuilder<C> builder,
        AsyncAPI kafkaApi)
    {
        Map<String, Object> channels = kafkaApi.getChannels();
        for (Map.Entry<String, Object> entry : channels.entrySet())
        {
            Channel channel = (Channel) entry.getValue();

            String channelName = entry.getKey();
            boolean compact = isTopicCompact(channel);
            String name = matcher.reset(channelName).replaceFirst(m -> m.group(2));
            String label = name.toUpperCase();

            injectHttpOperations(builder, name, label, compact);
        }

        return builder;
    }

    private void addReadWriteScopes(
        String label)
    {
        String base = label.toLowerCase();
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

        injectPostOperation(builder, name, label, compact);

        if (compact)
        {
            injectPutOperation(builder, name, label);
        }

        injectGetOperations(builder, name, label, identity);
        injectSseOperations(builder, name, label, identity);

        return builder;
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
                .map(r -> "protobuf".equals(r.type)
                    ? kafkaHelper.extractIdentityFieldFromProtobufSchema(r.schema)
                    : kafkaHelper.extractIdentityFieldFromSchema(r.schema))
                .orElse(null);
        }
        catch (Exception e)
        {
            System.out.println("Failed to resolve identity field for: " + label);
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
        String label)
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

        Map<String, Object> base = new HashMap<>();
        base.put("http", get);
        if (identity != null)
        {
            AsyncapiKafkaFilter f = new AsyncapiKafkaFilter();
            f.headers = Map.of("identity", "{identity}");
            ZillaHttpOperationBinding zh = new ZillaHttpOperationBinding(GET, null, List.of(f));
            base.put("x-zilla-http-kafka", zh);
        }

        Operation allGetOp = Operation.builder()
            .action(OperationAction.RECEIVE)
            .channel(new Reference("#/channels/" + name))
            .messages(Collections.singletonList(
                new Reference("#/channels/" + name + "/messages/" + label + "Messages")))
            .bindings(base)
            .security(List.of(new Reference("#/components/securitySchemes/httpOauth")))
            .build();

        String allOpName = "on%sGet".formatted(label);
        builder.addOperation(allOpName, allGetOp);

        Operation item = Operation.builder()
            .action(OperationAction.RECEIVE)
            .channel(new Reference("#/channels/" + name + "-item"))
            .messages(Collections.singletonList(
                new Reference("#/channels/" + name + "-item/messages/" + label + "Message")))
            .bindings(base)
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
        ZillaSseOperationBinding sse = new ZillaSseOperationBinding();
        Operation.OperationBuilder item = Operation.builder()
            .action(OperationAction.RECEIVE)
            .channel(new Reference("#/channels/%s-stream-identity".formatted(name)))
            .messages(Collections.singletonList(
                new Reference("#/channels/%s-stream-identity/messages/%sMessage".formatted(name, label))));

        AsyncapiKafkaFilter filter = new AsyncapiKafkaFilter();
        if (identity != null)
        {
            filter.key = "{identity}";
        }
        item
            .bindings(Map.of("x-zilla-sse", sse,
                "x-zilla-sse-kafka", new ZillaSseKafkaOperationBinding(List.of(filter))))
            .security(List.of(new Reference("#/components/securitySchemes/httpOauth")));
        String itemOpName = "on%sReadItem".formatted(label);
        builder.addOperation(itemOpName, item.build());

        Operation allOp = Operation.builder()
            .action(OperationAction.RECEIVE)
            .channel(new Reference("#/channels/%s-stream".formatted(name)))
            .messages(Collections.singletonList(
                new Reference("#/channels/%s-stream/messages/%sMessage".formatted(name, label))))
            .build();

        Map<String, Object> ab = new HashMap<>();
        ab.put("x-zilla-sse", sse);
        if (identity != null)
        {
            AsyncapiKafkaFilter cf = new AsyncapiKafkaFilter();
            cf.headers = Map.of("identity", "{identity}");
            ZillaSseKafkaOperationBinding sseAll = new ZillaSseKafkaOperationBinding(List.of(cf));
            ab.put("x-zilla-sse-kafka", sseAll);
        }
        allOp.setBindings(ab);
        allOp.setSecurity(List.of(new Reference("#/components/securitySchemes/httpOauth")));
        String allOpName = "on%sRead".formatted(label);
        builder.addOperation(allOpName, allOp);

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

    private String buildYaml(
        AsyncapiSpec spec) throws Exception
    {
        AsyncAPI asyncAPI = AsyncAPI.builder()
            .asyncapi(spec.version)
            .info(spec.info)
            .servers(spec.servers)
            .components(spec.components)
            .channels(spec.channels)
            .operations(spec.operations)
            .build();
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.setSerializationInclusion(NON_NULL);

        return yamlMapper.writeValueAsString(asyncAPI);
    }
}
