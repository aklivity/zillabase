package io.aklivity.zillabase.service.api.gen.internal.service;

import static io.aklivity.zillabase.service.api.gen.internal.service.AsyncapiSpecConfigService.KAFKA_ASYNCAPI_ARTIFACT_ID;

import java.util.ArrayList;
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

import org.springframework.stereotype.Service;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

@Service
public class KafkaAsyncApiService
{
    private final ApiGenConfig config;
    private final KafkaTopicSchemaService kafkaService;
    private final AsyncapiSpecConfigService specService;

    public KafkaAsyncApiService(
        ApiGenConfig config,
        KafkaTopicSchemaService kafkaService,
        AsyncapiSpecConfigService specService)
    {
        this.config = config;
        this.kafkaService = kafkaService;
        this.specService = specService;
    }

    public ApiGenEvent generate(
        ApiGenEvent event)
    {
        ApiGenEvent newEvent;

        try
        {
            List<KafkaTopicSchemaRecord> schemaRecords = kafkaService.resolve();

            String kafkaSpec = generateKafkaAsyncApiSpecs(schemaRecords);
            String specVersion = specService.register(KAFKA_ASYNCAPI_ARTIFACT_ID, kafkaSpec);

            newEvent = new ApiGenEvent(ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED, specVersion, null);
        }
        catch (Exception ex)
        {
            System.err.println("Error building Kafka AsyncApi Spec");
            ex.printStackTrace(System.err);

            newEvent = new ApiGenEvent(ApiGenEventType.KAFKA_ASYNC_API_ERRORED, null, null);
        }

        return newEvent;
    }

    private String generateKafkaAsyncApiSpecs(
        List<KafkaTopicSchemaRecord> schemaRecords) throws JsonProcessingException
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
        server.setHost(config.kafkaBootstrapServers());
        server.setProtocol("kafka");

        KafkaServerBinding kafkaServerBinding = new KafkaServerBinding();
        kafkaServerBinding.setSchemaRegistryUrl(config.karapaceUrl());
        kafkaServerBinding.setSchemaRegistryVendor("karapace");
        server.setBindings(Map.of("kafka", kafkaServerBinding));

        for (KafkaTopicSchemaRecord record : schemaRecords)
        {
            String topicName = record.name;
            String label = record.label;
            String subject = record.subject;
            String messageName = "%sMessage".formatted(label);

            String name = topicName;
            if (name.startsWith(config.risingwaveDb()))
            {
                name = name.replace("%s.".formatted(config.risingwaveDb()), "");
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

        return specService.build(info, components, channels, operations, Map.of("plain", server));
    }
}
