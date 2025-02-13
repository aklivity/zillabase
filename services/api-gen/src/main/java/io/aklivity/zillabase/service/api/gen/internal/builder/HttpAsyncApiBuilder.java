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
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationBinding;
import com.asyncapi.bindings.kafka.v0._5_0.channel.KafkaChannelBinding;
import com.asyncapi.bindings.kafka.v0._5_0.channel.KafkaChannelTopicCleanupPolicy;
import com.asyncapi.schemas.asyncapi.Reference;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuth2SecurityScheme;
import com.asyncapi.schemas.asyncapi.security.v3.oauth2.OAuthFlows;
import com.asyncapi.v3._0_0.model.AsyncAPI;
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
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiKafkaFilter;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaHttpOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaSseKafkaOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.ZillaSseOperationBinding;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;

public class HttpAsyncApiBuilder
{
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(\\w+)\\.(\\w+)");
    private final Matcher matcher = TOPIC_PATTERN.matcher("");
    private final ApiGenConfig config;
    private final KafkaTopicSchemaHelper kafkaHelper;
    private final List<String> scopes;

    public HttpAsyncApiBuilder(
        ApiGenConfig config,
        KafkaTopicSchemaHelper kafkaHelper)
    {
        this.config = config;
        this.kafkaHelper = kafkaHelper;
        this.scopes = new ArrayList<>();
    }

    public String buildSpec(
        String kafkaSpec) throws Exception
    {
        AsyncAPI kafkaApi = deserializeAsyncapi(kafkaSpec);

        AsyncapiSpecBuilder<AsyncapiSpec> builder = AsyncapiSpec.builder()
            .inject(spec -> spec.asyncapi("3.0.0"))
            .inject(this::injectInfo)
            .inject(this::injectServers)
            .inject(ctx -> injectChannels(ctx, kafkaApi))
            .inject(ctx -> createOperations(ctx, kafkaApi))
            .inject(this::injectComponents);

        AsyncapiSpec spec = builder.build();

        return buildYaml(spec);
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
                injectChannel(builder, channelName, (Channel) entry.getValue());
            }
        }

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectComponents(
        AsyncapiSpecBuilder<C> builder)
    {
        if (!scopes.isEmpty())
        {
            OAuth2SecurityScheme scheme = OAuth2SecurityScheme.oauth2Builder()
                .flows(new OAuthFlows())
                .scopes(scopes)
                .build();
            Map<String, Object> security = Map.of("httpOauth", scheme);
            builder.components(Components.builder()
                .securitySchemes(security)
                .build());
        }

        return builder;
    }

    private void createChannels(
        String name,
        String label,
        JsonValue val) throws JsonProcessingException
    {
        Map<String, Object> messagesRef = new HashMap<>();
        Map<String, Object> itemMessagesRef = new HashMap<>();

        populateMessageReferences(val, messagesRef, itemMessagesRef);

        Channel channel = new Channel();
        channel.setAddress("/" + name);
        channel.setMessages(messagesRef);
        ctx.channels.put(name, channel);

        channel = new Channel();
        channel.setAddress("/" + name + "/{id}");
        Parameter p = new Parameter();
        p.setDescription("Id of the item.");
        channel.setParameters(Map.of("id", p));
        channel.setMessages(itemMessagesRef);
        ctx.channels.put(name + "-item", channel);

        channel = new Channel();
        channel.setAddress("/" + name + "-stream");
        channel.setMessages(itemMessagesRef);
        ctx.channels.put(name + "-stream", channel);

        channel = new Channel();
        channel.setAddress("/" + name + "-stream-identity");
        channel.setMessages(itemMessagesRef);
        ctx.channels.put(name + "-stream-identity", channel);
    }

    private void parseMessagesAndSchemas(
        BuildContext context) throws JsonProcessingException
    {
        JsonObject componentsJson = context.kafkaJson.getJsonObject("components");
        if (componentsJson != null)
        {
            JsonObject msgs = componentsJson.getJsonObject("messages");
            JsonObject schs = componentsJson.getJsonObject("schemas");
            buildMessagesAndSchemas(msgs, schs, context);
        }
    }

    private <C> AsyncapiSpecBuilder<C> injectChannel(
        AsyncapiSpecBuilder<C> builder,
        String channelName,
        Channel channel)
    {
        String name = matcher.reset(channelName).replaceFirst(m -> m.group(2));
        String label = name.toUpperCase();

        addReadWriteScopes(label);
        createChannels(name, label, channel, ctx);

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectOperations(
        AsyncapiSpecBuilder<C> builder,
        AsyncAPI kafkaApi)
    {
        boolean compact = isTopicCompact(channel);

        injectHttpOperations(builder, name, label, compact);

        return builder;
    }

    private void addReadWriteScopes(
        String label)
    {
        String base = label.toLowerCase();
        scopes.add(base + ":read");
        scopes.add(base + ":write");
    }

    private void populateMessageReferences(
        JsonValue val,
        Map<String, Object> messagesRef,
        Map<String, Object> itemMessagesRef) throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonObject obj = val.asJsonObject();
        JsonObject kafkaMsgs = obj.getJsonObject("messages");
        if (kafkaMsgs != null)
        {
            for (Map.Entry<String, JsonValue> e : kafkaMsgs.entrySet())
            {
                JsonNode msgNode = mapper.readTree(e.getValue().toString());
                messagesRef.put(e.getKey(), msgNode);
                itemMessagesRef.put(e.getKey(), msgNode);
                JsonNode arrayMsg = msgNode.deepCopy();
                ((ObjectNode) arrayMsg).put("$ref", "#/components/messages/" + e.getKey() + "s");
                messagesRef.put(e.getKey() + "s", arrayMsg);
            }
        }
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

            compact = cleanupPolicy != null && cleanupPolicy.stream().anyMatch("compact"::equals);
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
            injectPutOperation(ctx, name, label);
        }

        injectGetOperations(ctx, name, label, identity);
        injectSseOperations(ctx, name, label, identity);

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
                new Reference("#/channels/%s-item/messages/%sMessage".formatted(name, label + "Message"))))
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
                new Reference("#/channels/%s-stream/messages/%sMessages".formatted(name, label))))
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

    private void buildMessagesAndSchemas(
        JsonObject msgs,
        JsonObject schs) throws JsonProcessingException
    {
        ObjectMapper m = new ObjectMapper();
        if (msgs != null)
        {
            for (Map.Entry<String, JsonValue> e : msgs.entrySet())
            {
                JsonNode om = m.readTree(e.getValue().toString());
                String k = e.getKey();
                ctx.messages.put(k, om);
                String ct = om.has("contentType") ? om.get("contentType").asText() : "application/json";
                String arrKey = k + "s";
                ObjectNode arrMsg = m.createObjectNode();
                arrMsg.put("name", arrKey);
                arrMsg.put("contentType", ct);
                String ref = om.path("payload").path("$ref").asText();
                ObjectNode pay = m.createObjectNode();
                pay.put("$ref", ref + "s");
                arrMsg.set("payload", pay);
                ctx.messages.put(arrKey, arrMsg);
            }
        }
        if (schs != null)
        {
            for (Map.Entry<String, JsonValue> e : schs.entrySet())
            {
                String k = e.getKey();
                JsonNode os = m.readTree(e.getValue().toString());
                ctx.schemas.put(k, os);
                String arrK = k + "s";
                ObjectNode arrS = m.createObjectNode();
                arrS.put("type", "array");
                ObjectNode it = m.createObjectNode();
                it.put("$ref", "#/components/schemas/" + k);
                arrS.set("items", it);
                arrS.put("name", arrK.replace(config.risingwaveDb() + ".", ""));
                arrS.put("namespace", config.risingwaveDb());
                ctx.schemas.put(arrK, arrS);
            }
        }
    }

    private AsyncAPI deserializeAsyncapi(
        String yaml) throws Exception
    {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        return yamlMapper.readValue(yaml, AsyncAPI.class);
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
