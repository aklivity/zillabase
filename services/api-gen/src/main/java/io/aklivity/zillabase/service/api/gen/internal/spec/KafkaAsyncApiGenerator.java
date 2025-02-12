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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.aklivity.zillabase.service.api.gen.internal.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelBinding;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelTopicCleanupPolicy;
import com.asyncapi.bindings.kafka.v0._4_0.channel.KafkaChannelTopicConfiguration;
import com.asyncapi.bindings.kafka.v0._4_0.server.KafkaServerBinding;
import com.asyncapi.schemas.asyncapi.Reference;
import com.asyncapi.v2._6_0.model.channel.message.Message;
import com.asyncapi.v3._0_0.model.channel.Channel;
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

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KafkaConfig;

public class KafkaAsyncApiGenerator
{
    private final ApiGenConfig config;
    private final KafkaConfig kafkaConfig;

    public KafkaAsyncApiGenerator(
        ApiGenConfig config,
        KafkaConfig kafkaConfig)
    {
        this.config = config;
        this.kafkaConfig = kafkaConfig;
    }

    public String buildYamlSpec(
        List<KafkaTopicSchemaRecord> schemaRecords) throws Exception
    {
        AsyncapiSpecBuilder<AsyncapiSpec> builder = AsyncapiSpec.builder()
            .asyncapi("3.0.0")
            .info(createInfo())
            .servers(createServers())
            .components()
            .channels()
            .operations()
            .messages()
            .schemas();

        return builder.buildYaml();
    }

    private void buildChannelsAndOperations(List<KafkaTopicSchemaRecord> records)
        throws JsonProcessingException
    {
        for (KafkaTopicSchemaRecord record : records)
        {
            createChannelMessageOperation(ctx, record);
        }
        ctx.components.setSchemas(ctx.schemas);
        ctx.components.setMessages(ctx.messages);
    }

    private Info createInfo()
    {
        Info info = new Info();
        info.setTitle("API Document for Kafka Cluster");
        info.setVersion("1.0.0");

        License license = new License(
            "Aklivity Community License",
            "https://github.com/aklivity/zillabase/blob/develop/LICENSE");
        info.setLicense(license);

        return info;
    }

    private Map<String, Object> createServers()
    {
        Server server = new Server();
        server.setHost(kafkaConfig.bootstrapServers());
        server.setProtocol("kafka");

        KafkaServerBinding binding = new KafkaServerBinding();
        binding.setSchemaRegistryUrl(kafkaConfig.karapaceUrl());
        binding.setSchemaRegistryVendor("karapace");
        server.setBindings(Map.of("kafka", binding));

        return Map.of("plain", server);
    }

    private void createChannelMessageOperation(KafkaTopicSchemaRecord record)
        throws JsonProcessingException
    {
        String topicName = record.name;
        String label = record.label;
        String subject = record.subject;
        String messageName = label + "Message";

        String safeName = stripDbPrefixIfNeeded(topicName);

        Channel channel = createChannel(topicName, record.cleanupPolicies);
        Reference channelMessageRef = new Reference("#/components/messages/%s".formatted(messageName));
        channel.setMessages(Map.of(messageName, channelMessageRef));
        ctx.channels.put(safeName, channel);

        JsonNode schemaObject = buildPayloadSchema(record.schema);
        ctx.schemas.put(subject, schemaObject);

        Message message = createMessage(messageName, record.type, subject);
        ctx.messages.put(messageName, message);

        Operation sendOp = createSendOperation(safeName, messageName, topicName);
        ctx.operations.put("do" + label, sendOp);

        Operation receiveOp = createReceiveOperation(safeName, messageName);
        ctx.operations.put("on" + label, receiveOp);
    }

    private String stripDbPrefixIfNeeded(String topicName)
    {
        String prefix = config.risingwaveDb() + ".";
        if (topicName.startsWith(prefix))
        {
            return topicName.replace(prefix, "");
        }
        return topicName;
    }

    private Channel createChannel(String topicName, List<String> cleanupPolicies)
    {
        Channel channel = new Channel();
        channel.setAddress(topicName);

        KafkaChannelBinding binding = new KafkaChannelBinding();
        KafkaChannelTopicConfiguration config = new KafkaChannelTopicConfiguration();
        List<KafkaChannelTopicCleanupPolicy> policies = new ArrayList<>();
        for (String policy : cleanupPolicies)
        {
            policies.add(KafkaChannelTopicCleanupPolicy.valueOf(policy.toUpperCase()));
        }
        config.setCleanupPolicy(policies);

        binding.setTopicConfiguration(config);
        channel.setBindings(Map.of("kafka", binding));
        return channel;
    }

    private JsonNode buildPayloadSchema(String rawSchema)
        throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(rawSchema);
        if (node.has("type") && "record".equals(node.get("type").asText()))
        {
            ((ObjectNode) node).put("type", "object");
        }
        return node;
    }

    private Message createMessage(String messageName, String mediaType, String subjectRef)
    {
        Message msg = new Message();
        msg.setName(messageName);
        msg.setContentType("application/" + mediaType);
        msg.setPayload(new Reference("#/components/schemas/" + subjectRef));
        return msg;
    }

    private Operation createSendOperation(String channelName, String messageName, String originalTopicName)
    {
        Operation op = new Operation();
        op.setAction(OperationAction.SEND);
        op.setChannel(new Reference("#/channels/" + channelName));
        op.setMessages(Collections.singletonList(
            new Reference("#/channels/" + channelName + "/messages/" + messageName)));

        if (originalTopicName.endsWith("_commands"))
        {
            String replyTopic = channelName.replace("_commands", "_replies");
            OperationReply reply = new OperationReply();
            reply.setChannel(new Reference("#/channels/" + replyTopic));
            op.setReply(reply);
        }
        return op;
    }

    private Operation createReceiveOperation(String channelName, String messageName)
    {
        Operation op = new Operation();
        op.setAction(OperationAction.RECEIVE);
        op.setChannel(new Reference("#/channels/" + channelName));
        op.setMessages(Collections.singletonList(
            new Reference("#/channels/" + channelName + "/messages/" + messageName)));
        return op;
    }
}
