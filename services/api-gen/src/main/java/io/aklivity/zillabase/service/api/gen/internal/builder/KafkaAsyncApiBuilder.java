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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

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
import com.asyncapi.v3._0_0.model.AsyncAPI;
import com.asyncapi.v3._0_0.model.channel.Channel;
import com.asyncapi.v3._0_0.model.channel.message.Message;
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
import io.aklivity.zillabase.service.api.gen.internal.config.KafkaConfig;

public class KafkaAsyncApiBuilder
{
    private final KafkaConfig kafkaConfig;

    public KafkaAsyncApiBuilder(
        KafkaConfig kafkaConfig)
    {
        this.kafkaConfig = kafkaConfig;
    }

    public String buildSpec(
        List<KafkaTopicSchemaRecord> schemaRecords) throws Exception
    {
        AsyncapiSpecBuilder<AsyncapiSpec> builder = AsyncapiSpec.builder()
            .inject(spec -> spec.asyncapi("3.0.0"))
            .inject(this::injectInfo)
            .inject(this::injectServers)
            .inject(a -> injectChannels(a, schemaRecords))
            .inject(a -> injectOperations(a, schemaRecords))
            .inject(a -> injectComponents(a, schemaRecords));

        AsyncapiSpec spec = builder.build();

        return buildYaml(spec);
    }

    private <C> AsyncapiSpecBuilder<C> injectInfo(
        AsyncapiSpecBuilder<C> builder)
    {
        Info info = Info.builder()
            .title("API Document for Kafka Cluster")
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
        Server server = Server.builder()
            .host(kafkaConfig.bootstrapServers())
            .protocol("kafka")
            .bindings(Map.of("kafka", KafkaServerBinding.builder()
                .schemaRegistryUrl(kafkaConfig.karapaceUrl())
                .schemaRegistryVendor("karapace")
                .build()))
            .build();

        return builder.servers(Map.of("plain", server));
    }

    private <C> AsyncapiSpecBuilder<C> injectComponents(
        AsyncapiSpecBuilder<C> builder,
        List<KafkaTopicSchemaRecord> records)
    {
        Map<String, Object> messages = new HashMap<>();
        Map<String, Object> schemas = new HashMap<>();

        for (KafkaTopicSchemaRecord record : records)
        {
            createSchemas(schemas, record);
            createMessages(messages, record);
        }

        Components components = Components.builder()
            .schemas(schemas)
            .messages(messages)
            .build();

        builder.components(components);

        return builder;
    }

    private void createMessages(
        Map<String, Object> messages,
        KafkaTopicSchemaRecord record)
    {
        String label = record.label;
        String subject = record.subject;
        String messageName = "%sMessage".formatted(label);

        Message message = Message.builder()
            .name(messageName)
            .contentType("application/" + record.type)
            .payload(new Reference("#/components/schemas/" + subject))
            .build();

        messages.put(messageName, message);
    }

    private void createSchemas(
        Map<String, Object> schemas,
        KafkaTopicSchemaRecord record)
    {
        String topicName = record.name;
        String subject = record.subject;

        try
        {
            JsonNode schemaObject = buildPayloadSchema(record.schema);
            schemas.put(subject, schemaObject);
        }
        catch (JsonProcessingException e)
        {
            System.out.println("Failed to build schema for " + topicName);
        }
    }

    private <C> AsyncapiSpecBuilder<C> injectChannels(
        AsyncapiSpecBuilder<C> builder,
        List<KafkaTopicSchemaRecord> records)
    {
        Map<String, Object> channels = new HashMap<>();

        for (KafkaTopicSchemaRecord record : records)
        {
            String topicName = record.name;
            String safeName = stripSchemaPrefix(topicName);
            List<String> cleanupPolicies = record.cleanupPolicies;
            String label = record.label;
            String messageName = "%sMessage".formatted(label);

            Channel channel = createChannel(topicName, messageName, cleanupPolicies);

            channels.put(safeName, channel);
        }

        builder.channels(channels);

        return builder;
    }

    private <C> AsyncapiSpecBuilder<C> injectOperations(
        AsyncapiSpecBuilder<C> builder,
        List<KafkaTopicSchemaRecord> records)
    {
        Map<String, Object> operations = new HashMap<>();

        for (KafkaTopicSchemaRecord record : records)
        {
            String topicName = record.name;
            String safeName = stripSchemaPrefix(topicName);
            String label = record.label;
            String messageName = "%sMessage".formatted(label);

            Operation sendOperation = createSendOperation(safeName, messageName, topicName);
            Operation receiveOperation = createReceiveOperation(safeName, messageName);

            operations.put("do%s".formatted(label), sendOperation);
            operations.put("on%s".formatted(label), receiveOperation);
        }

        builder.operations(operations);

        return builder;
    }

    private String stripSchemaPrefix(
        String topicName)
    {
        return topicName.replaceAll("^[^.]+\\.", "");
    }

    private Channel createChannel(
        String topicName,
        String messageName,
        List<String> cleanupPolicies)
    {
        List<KafkaChannelTopicCleanupPolicy> policies = new ArrayList<>();

        for (String policy : cleanupPolicies)
        {
            policies.add(KafkaChannelTopicCleanupPolicy.valueOf(policy.toUpperCase()));
        }

        KafkaChannelTopicConfiguration config = KafkaChannelTopicConfiguration.builder()
            .cleanupPolicy(policies)
            .build();

        KafkaChannelBinding binding = KafkaChannelBinding.builder()
            .topicConfiguration(config)
            .build();

        Reference reference = new Reference("#/components/messages/%s".formatted(messageName));

        return Channel.builder()
            .address(topicName)
            .bindings(Map.of("kafka", binding))
            .messages(Map.of(messageName, reference))
            .build();
    }

    private JsonNode buildPayloadSchema(
        String rawSchema) throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(rawSchema);

        if (node.has("type") && "record".equals(node.get("type").asText()))
        {
            ((ObjectNode) node).put("type", "object");
        }

        return node;
    }

    private Operation createSendOperation(
        String channelName,
        String messageName,
        String originalTopicName)
    {
        Operation.OperationBuilder op = Operation.builder()
            .action(OperationAction.SEND)
            .channel(new Reference("#/channels/" + channelName))
            .messages(Collections.singletonList(
                new Reference("#/channels/" + channelName + "/messages/" + messageName))
            );

        if (originalTopicName.endsWith("_commands"))
        {
            String replyTopic = channelName.replace("_commands", "_replies");
            op.reply(OperationReply.builder()
                .channel(new Reference("#/channels/" + replyTopic))
                .build());
        }

        return op.build();
    }

    private Operation createReceiveOperation(
        String channelName,
        String messageName)
    {
        return Operation.builder()
            .action(OperationAction.RECEIVE)
            .channel(new Reference("#/channels/" + channelName))
            .messages(Collections.singletonList(
                new Reference("#/channels/" + channelName + "/messages/" + messageName))
            )
            .build();
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
