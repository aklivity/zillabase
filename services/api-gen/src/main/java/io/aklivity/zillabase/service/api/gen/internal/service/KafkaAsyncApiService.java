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
package io.aklivity.zillabase.service.api.gen.internal.service;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.KAFKA_ASYNCAPI_ARTIFACT_ID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

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

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

@Service
public class KafkaAsyncApiService
{
    private final ApiGenConfig config;
    private final KafkaTopicSchemaHelper kafkaHelper;
    private final ApicurioHelper specHelper;

    public KafkaAsyncApiService(
        ApiGenConfig config,
        KafkaTopicSchemaHelper kafkaHelper,
        ApicurioHelper specHelper)
    {
        this.config = config;
        this.kafkaHelper = kafkaHelper;
        this.specHelper = specHelper;
    }

    public ApiGenEvent generate(
        ApiGenEvent event)
    {
        ApiGenEventType eventType;
        String specVersion = null;

        try
        {
            List<KafkaTopicSchemaRecord> schemaRecords = kafkaHelper.resolve();
            String kafkaSpec = generateKafkaAsyncApiSpecs(schemaRecords);

            if (kafkaSpec != null)
            {
                eventType = ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED;
                specVersion = specHelper.publishSpec(KAFKA_ASYNCAPI_ARTIFACT_ID, kafkaSpec);
            }
            else
            {
                eventType = ApiGenEventType.KAFKA_ASYNC_API_ERRORED;
            }
        }
        catch (Exception ex)
        {
            eventType = ApiGenEventType.KAFKA_ASYNC_API_ERRORED;
        }

        return new ApiGenEvent(eventType, specVersion, null);
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

        return build(info, components, channels, operations, Map.of("plain", server));
    }

    private String build(
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

        return mapper.writeValueAsString(asyncAPI);
    }
}
