package io.aklivity.zillabase.service.api.gen.internal.service;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;

import java.io.StringReader;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelBinding;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelTopicCleanupPolicy;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelTopicConfiguration;
import com.asyncapi.bindings.kafka.v0._4_0.server.KafkaServerBinding;
import com.asyncapi.schemas.asyncapi.Reference;
import com.asyncapi.v2._6_0.model.channel.message.Message;
import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.component.Components;
import com.asyncapi.v3._0_0.model.info.Info;
import com.asyncapi.v3._0_0.model.info.License;
import com.asyncapi.v3._0_0.model.operation.Operation;
import com.asyncapi.v3._0_0.model.operation.OperationAction;
import com.asyncapi.v3._0_0.model.operation.reply.OperationReply;
import com.asyncapi.v3._0_0.model.server.Server;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.serde.Event;

@Service
public class KafkaAsyncApiService extends AsyncapiService
{
    private static final String KAFKA_ASYNCAPI_ARTIFACT_ID = "kafka-asyncapi";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private final List<KafkaTopicSchemaRecord> records = new ArrayList<>();

    public Event generate(
        Event event)
    {
        String kafkaSpec = null;

        resolveKafkaTopicsAndSchemas();

        if (!records.isEmpty())
        {
            kafkaSpec = generateKafkaAsyncApiSpecs();
        }

        if (kafkaSpec != null)
        {
            registerAsyncApiSpec(KAFKA_ASYNCAPI_ARTIFACT_ID, kafkaSpec);
        }

        return new Event();
    }

    private void resolveKafkaTopicsAndSchemas()
    {
        try (AdminClient adminClient = AdminClient.create(Map.of(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)))
        {
            KafkaFuture<Collection<TopicListing>> topics = adminClient.listTopics().listings();
            for (TopicListing topic : topics.get())
            {
                if (!topic.isInternal())
                {
                    String topicName = topic.name();

                    ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                    DescribeConfigsResult result = adminClient.describeConfigs(List.of(resource));
                    Map<ConfigResource, Config> configMap = result.all().get();

                    Config topicConfig = configMap.get(resource);
                    String[] policies = topicConfig.get(CLEANUP_POLICY_CONFIG).value().split(",");

                    String subject = "%s-value".formatted(topicName);
                    String schema = resolveSchema(client, subject);
                    if (schema != null)
                    {
                        JsonReader reader = Json.createReader(new StringReader(schema));
                        JsonObject object = reader.readObject();

                        if (object.containsKey("schema"))
                        {
                            String schemaStr = object.getString("schema");
                            String type = resolveType(schemaStr);
                            records.add(new KafkaTopicSchemaRecord(topicName, policies,
                                matcher.reset(topicName.replace("%s.".formatted(config.risingwave.db), ""))
                                    .replaceAll(match -> match.group(2).toUpperCase()),
                                subject, type, schemaStr));
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            System.err.println("Error resolving Kafka Topics & Schemas Info");
        }
    }

    private String generateKafkaAsyncApiSpecs()
    {
        String spec = null;
        try
        {
            final Components components = new Components();
            final Map<String, Object> schemas = new HashMap<>();
            final Map<String, Object> messages = new HashMap<>();
            final Map<String, Object> channels = new HashMap<>();
            final Map<String, Object> operations = new HashMap<>();

            Message message;
            Channel channel;
            Operation operation;
            Reference reference;

            Info info = new Info();
            info.setTitle("API Document for Kafka Cluster");
            info.setVersion("1.0.0");
            License license = new License("Aklivity Community License",
                "https://github.com/aklivity/zillabase/blob/develop/LICENSE");
            info.setLicense(license);

            Server server = new Server();
            server.setHost(config.kafka.bootstrapUrl);
            server.setProtocol("kafka");

            KafkaServerBinding kafkaServerBinding = new KafkaServerBinding();
            kafkaServerBinding.setSchemaRegistryUrl(config.registry.karapace.url);
            kafkaServerBinding.setSchemaRegistryVendor("karapace");
            server.setBindings(Map.of("kafka", kafkaServerBinding));

            for (KafkaTopicSchemaRecord record : records)
            {
                String topicName = record.name;
                String label = record.label;
                String subject = record.subject;
                String messageName = "%sMessage".formatted(label);

                String name = topicName;
                if (name.startsWith(config.risingwave.db))
                {
                    name = name.replace("%s.".formatted(config.risingwave.db), "");
                }

                channel = new Channel();
                channel.setAddress(topicName);
                KafkaChannelBinding kafkaChannelBinding = new KafkaChannelBinding();
                KafkaChannelTopicConfiguration topicConfiguration = new KafkaChannelTopicConfiguration();
                List<KafkaChannelTopicCleanupPolicy> policies = new ArrayList<>();
                for (String policy : record.cleanupPolicies)
                {
                    policies.add(KafkaChannelTopicCleanupPolicy.valueOf(policy.toUpperCase()));
                }
                topicConfiguration.setCleanupPolicy(policies);
                kafkaChannelBinding.setTopicConfiguration(topicConfiguration);
                channel.setBindings(Map.of("kafka", kafkaChannelBinding));
                reference = new Reference("#/components/messages/%s".formatted(messageName));
                channel.setMessages(Map.of(messageName, reference));
                channels.put(name, channel);

                ObjectMapper schemaMapper = new ObjectMapper();
                JsonNode schemaObject = schemaMapper.readTree(record.schema);
                if ("record".equals(schemaObject.get("type").asText()))
                {
                    ((ObjectNode) schemaObject).put("type", "object");
                }
                schemas.put(subject, schemaObject);

                message = new Message();
                message.setName(messageName);
                message.setContentType("application/%s".formatted(record.type));

                reference = new Reference("#/components/schemas/%s".formatted(subject));
                message.setPayload(reference);
                messages.put(messageName, message);

                operation = new Operation();
                operation.setAction(OperationAction.SEND);
                reference = new Reference("#/channels/%s".formatted(name));
                operation.setChannel(reference);
                reference = new Reference("#/channels/%s/messages/%s".formatted(name, messageName));
                operation.setMessages(Collections.singletonList(reference));
                if (name.endsWith("_commands"))
                {
                    String replyTopic = name.replace("_commands", "_replies_sink");
                    OperationReply reply = new OperationReply();
                    reference = new Reference("#/channels/%s".formatted(replyTopic));
                    reply.setChannel(reference);
                    operation.setReply(reply);
                }

                operations.put("do%s".formatted(label), operation);

                operation = new Operation();
                operation.setAction(OperationAction.RECEIVE);
                reference = new Reference("#/channels/%s".formatted(name));
                operation.setChannel(reference);
                reference = new Reference("#/channels/%s/messages/%s".formatted(name, messageName));
                operation.setMessages(Collections.singletonList(reference));
                operations.put("on%s".formatted(label), operation);
            }

            components.setSchemas(schemas);
            components.setMessages(messages);

            spec = buildAsyncApiSpec(info, components, channels, operations, Map.of("plain", server));
        }
        catch (Exception ex)
        {
            System.err.println("Error generating Kafka AsyncApi Spec");
            ex.printStackTrace(System.err);
        }
        return spec;
    }

    private String buildAsyncApiSpec(
        Info info,
        Components components,
        Map<String, Object> channels,
        Map<String, Object> operations,
        Map<String, Object> servers)
    {
        String spec = null;
        try
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

            spec = mapper.writeValueAsString(asyncAPI);
        }
        catch (JsonProcessingException ex)
        {
            System.err.println("Error building AsyncApi Spec");
            ex.printStackTrace(System.err);
        }
        return spec;
    }
}
