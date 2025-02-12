package io.aklivity.zillabase.service.api.gen.internal.spec;

import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.GET;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.POST;
import static com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationMethod.PUT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import com.asyncapi.bindings.http.v0._3_0.operation.HTTPOperationBinding;
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
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
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

    public String buildYamlSpec(
        String kafkaAsyncApiYaml) throws JsonProcessingException, ExecutionException, InterruptedException
    {
        BuildContext context = new BuildContext();
        parseInput(kafkaAsyncApiYaml, context);
        parseChannels(context);
        parseMessagesAndSchemas(context);
        buildSecurity(context);
        return finalizeSpec(context);
    }

    private void parseInput(
        String kafkaAsyncapi,
        BuildContext context) throws JsonProcessingException
    {
        context.components = new Components();
        context.servers = buildServers();
        context.info = buildInfo();
        context.channels = new HashMap<>();
        context.operations = new HashMap<>();
        context.messages = new HashMap<>();
        context.schemas = new HashMap<>();
        scopes.clear();

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode yamlRoot = yamlMapper.readTree(kafkaAsyncapi);
        ObjectMapper jsonMapper = new ObjectMapper();
        String asJsonString = jsonMapper.writeValueAsString(yamlRoot);
        context.kafkaJson = Json.createReader(new StringReader(asJsonString)).readObject();
    }

    private void parseChannels(
        BuildContext context) throws JsonProcessingException, ExecutionException, InterruptedException
    {
        JsonObject channelsJson = context.kafkaJson.getJsonObject("channels");
        if (channelsJson != null)
        {
            for (Map.Entry<String, JsonValue> entry : channelsJson.entrySet())
            {
                String channelName = entry.getKey();
                if (!channelName.endsWith("_replies"))
                {
                    processChannel(channelName, entry.getValue(), context);
                }
            }
        }
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

    private void buildSecurity(
        BuildContext context)
    {
        if (!scopes.isEmpty())
        {
            OAuth2SecurityScheme scheme = OAuth2SecurityScheme.oauth2Builder()
                .flows(new OAuthFlows())
                .scopes(scopes)
                .build();
            Map<String, Object> sec = Map.of("httpOauth", scheme);
            context.components.setSecuritySchemes(sec);
        }
    }

    private String finalizeSpec(
        BuildContext context) throws JsonProcessingException
    {
        AsyncAPI api = new AsyncAPI();
        api.setAsyncapi("3.0.0");
        api.setInfo(context.info);
        api.setServers(context.servers);
        api.setComponents(context.components);
        api.setChannels(context.channels);
        api.setOperations(context.operations);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).setSerializationInclusion(NON_NULL);
        return mapper.writeValueAsString(api);
    }

    private Map<String, Object> buildServers()
    {
        boolean secure = true;
        Reference securityRef = new Reference("#/components/securitySchemes/httpOauth");
        Server server = new Server();
        server.setHost("localhost:8080");
        server.setProtocol("http");
        if (secure)
        {
            server.setSecurity(List.of(securityRef));
        }

        return Map.of("http", server);
    }

    private Info buildInfo()
    {
        Info i = new Info();
        i.setTitle("API Document for REST APIs");
        i.setVersion("1.0.0");
        License license = new License("Aklivity Community License",
            "https://github.com/aklivity/zillabase/blob/develop/LICENSE");
        i.setLicense(license);
        return i;
    }

    private void processChannel(
        String channelName,
        JsonValue channelVal,
        BuildContext ctx) throws JsonProcessingException, ExecutionException, InterruptedException
    {
        String name = matcher.reset(channelName).replaceFirst(m -> m.group(2));
        String label = name.toUpperCase();
        addReadWriteScopes(label);
        createChannels(name, label, channelVal, ctx);

        boolean compact = isCompactTopic(channelVal);
        generateHttpOperations(ctx, name, label, compact);
    }

    private void addReadWriteScopes(
        String label)
    {
        String base = label.toLowerCase();
        scopes.add(base + ":read");
        scopes.add(base + ":write");
    }

    private void createChannels(
        String name,
        String label,
        JsonValue val,
        BuildContext ctx) throws JsonProcessingException
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

    private boolean isCompactTopic(
        JsonValue channelVal)
    {
        boolean compact = false;
        JsonObject binding = channelVal.asJsonObject().getJsonObject("bindings");

        if (binding != null && binding.containsKey("kafka"))
        {
            JsonObject k = binding.getJsonObject("kafka");
            JsonObject cfg = k.getJsonObject("topicConfiguration");
            if (cfg != null)
            {
                JsonArray policies = cfg.getJsonArray("cleanup.policy");
                if (policies != null)
                {
                    for (JsonValue policy : policies)
                    {
                        if ("compact".equals(policy.toString().replace("\"", "")))
                        {
                            compact = true;
                            break;
                        }
                    }
                }
            }
        }

        return compact;
    }

    private void generateHttpOperations(
        BuildContext ctx,
        String name,
        String label,
        boolean compact) throws JsonProcessingException, ExecutionException, InterruptedException
    {
        String identity = resolveIdentityField(label);
        createPostOperation(ctx, name, label, compact);

        if (compact)
        {
            createPutOperation(ctx, name, label);
        }

        createGetOperations(ctx, name, label, identity);
        createSseOperations(ctx, name, label, identity);
    }

    private String resolveIdentityField(
        String label) throws ExecutionException, InterruptedException, JsonProcessingException
    {
        String found = null;
        List<KafkaTopicSchemaRecord> records = kafkaHelper.resolve();
        for (KafkaTopicSchemaRecord r : records)
        {
            if (label.equals(r.label))
            {
                found = ("protobuf".equals(r.type))
                    ? kafkaHelper.extractIdentityFieldFromProtobufSchema(r.schema)
                    : kafkaHelper.extractIdentityFieldFromSchema(r.schema);
                break;
            }
        }
        return found;
    }

    private void createPostOperation(
        BuildContext ctx,
        String name,
        String label,
        boolean compact)
    {
        Operation op = new Operation();
        op.setAction(OperationAction.SEND);
        op.setChannel(new Reference("#/channels/" + name));
        op.setMessages(Collections.singletonList(
            new Reference("#/channels/" + name + "/messages/" + label + "Message")));
        HTTPOperationBinding http = new HTTPOperationBinding();
        http.setMethod(POST);
        Map<String, Object> b = new HashMap<>();
        b.put("http", http);
        ZillaHttpOperationBinding zilla = new ZillaHttpOperationBinding(POST,
            Map.of("zilla:identity", "{identity}"), null);
        b.put("x-zilla-http-kafka", zilla);
        op.setSecurity(List.of(new Reference("#/components/securitySchemes/httpOauth")));
        op.setBindings(b);
        String n = compact ? "do" + label + "Create" : "do" + label;
        ctx.operations.put(n, op);
    }

    private void createPutOperation(
        BuildContext ctx,
        String name,
        String label)
    {
        Operation op = new Operation();
        op.setAction(OperationAction.SEND);
        op.setChannel(new Reference("#/channels/" + name + "-item"));
        op.setMessages(Collections.singletonList(
            new Reference("#/channels/" + name + "-item/messages/" + label + "Message")));
        HTTPOperationBinding put = new HTTPOperationBinding();
        put.setMethod(PUT);
        Map<String, Object> b = new HashMap<>();
        b.put("http", put);
        ZillaHttpOperationBinding zilla = new ZillaHttpOperationBinding(PUT,
            Map.of("zilla:identity", "{identity}"), null);
        b.put("x-zilla-http-kafka", zilla);
        op.setSecurity(List.of(new Reference("#/components/securitySchemes/httpOauth")));
        op.setBindings(b);
        ctx.operations.put("do" + label + "Update", op);
    }

    private void createGetOperations(
        BuildContext ctx,
        String name,
        String label,
        String identity)
    {
        HTTPOperationBinding get = new HTTPOperationBinding();
        get.setMethod(GET);
        Map<String, Object> base = new HashMap<>();
        base.put("http", get);
        if (identity != null)
        {
            AsyncapiKafkaFilter f = new AsyncapiKafkaFilter();
            f.headers = Map.of("identity", "{identity}");
            ZillaHttpOperationBinding zh = new ZillaHttpOperationBinding(GET, null, List.of(f));
            base.put("x-zilla-http-kafka", zh);
        }
        Operation all = new Operation();
        all.setAction(OperationAction.RECEIVE);
        all.setChannel(new Reference("#/channels/" + name));
        all.setMessages(List.of(
            new Reference("#/channels/" + name + "/messages/" + label + "Messages")));
        all.setBindings(base);
        all.setSecurity(List.of(new Reference("#/components/securitySchemes/httpOauth")));
        ctx.operations.put("on" + label + "Get", all);
        Operation item = new Operation();
        item.setAction(OperationAction.RECEIVE);
        item.setChannel(new Reference("#/channels/" + name + "-item"));
        item.setMessages(List.of(
            new Reference("#/channels/" + name + "-item/messages/" + label + "Message")));
        item.setBindings(base);
        item.setSecurity(List.of(new Reference("#/components/securitySchemes/httpOauth")));
        ctx.operations.put("on" + label + "GetItem", item);
    }

    private void createSseOperations(
        BuildContext ctx,
        String name,
        String label,
        String identity)
    {
        ZillaSseOperationBinding sse = new ZillaSseOperationBinding();
        Operation item = new Operation();
        item.setAction(OperationAction.RECEIVE);
        item.setChannel(new Reference("#/channels/" + name + "-stream-identity"));
        item.setMessages(List.of(
            new Reference("#/channels/" + name + "-stream-identity/messages/" + label + "Message")));
        AsyncapiKafkaFilter sf = new AsyncapiKafkaFilter();
        if (identity != null)
        {
            sf.key = "{identity}";
        }
        ZillaSseKafkaOperationBinding sskb = new ZillaSseKafkaOperationBinding(List.of(sf));
        Map<String, Object> ib = Map.of("x-zilla-sse", sse, "x-zilla-sse-kafka", sskb);
        item.setBindings(ib);
        item.setSecurity(List.of(new Reference("#/components/securitySchemes/httpOauth")));
        ctx.operations.put("on" + label + "ReadItem", item);
        Operation all = new Operation();
        all.setAction(OperationAction.RECEIVE);
        all.setChannel(new Reference("#/channels/" + name + "-stream"));
        all.setMessages(List.of(
            new Reference("#/channels/" + name + "-stream/messages/" + label + "Message")));
        Map<String, Object> ab = new HashMap<>();
        ab.put("x-zilla-sse", sse);
        if (identity != null)
        {
            AsyncapiKafkaFilter cf = new AsyncapiKafkaFilter();
            cf.headers = Map.of("identity", "{identity}");
            ZillaSseKafkaOperationBinding sseAll = new ZillaSseKafkaOperationBinding(List.of(cf));
            ab.put("x-zilla-sse-kafka", sseAll);
        }
        all.setBindings(ab);
        all.setSecurity(List.of(new Reference("#/components/securitySchemes/httpOauth")));
        ctx.operations.put("on" + label + "Read", all);
    }

    private void buildMessagesAndSchemas(
        JsonObject msgs,
        JsonObject schs,
        BuildContext ctx) throws JsonProcessingException
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

    private static final class BuildContext
    {
        private Components components;
        private Map<String, Object> servers;
        private Info info;
        private JsonObject kafkaJson;
        private Map<String, Object> channels;
        private Map<String, Object> operations;
        private Map<String, Object> messages;
        private Map<String, Object> schemas;
    }
}
