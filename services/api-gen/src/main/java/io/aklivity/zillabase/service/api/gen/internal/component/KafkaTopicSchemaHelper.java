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
package io.aklivity.zillabase.service.api.gen.internal.component;

import static org.apache.kafka.common.config.TopicConfig.CLEANUP_POLICY_CONFIG;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;

@Service
public class KafkaTopicSchemaHelper
{
    private static final Pattern PROTO_MESSAGE_PATTERN = Pattern.compile("message\\s+\\w+\\s*\\{[^}]*\\}",
        Pattern.DOTALL);
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(^|-|_)(.)");

    private final Matcher protoMatcher = PROTO_MESSAGE_PATTERN.matcher("");
    private final Matcher matcher = TOPIC_PATTERN.matcher("");

    private final ApiGenConfig config;
    private final AdminClient adminClient;

    private final List<KafkaTopicSchemaRecord> records;
    private final WebClient webClient;

    KafkaTopicSchemaHelper(
        ApiGenConfig config,
        AdminClient adminClient,
        WebClient webClient)
    {
        this.config = config;
        this.adminClient = adminClient;
        this.webClient = webClient;
        this.records = new ArrayList<>();
    }

    public List<KafkaTopicSchemaRecord> resolve() throws ExecutionException, InterruptedException, JsonProcessingException
    {
        records.clear();

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
                String schema = resolveSchema(subject);
                if (schema != null)
                {
                    JsonReader reader = Json.createReader(new StringReader(schema));
                    JsonObject object = reader.readObject();

                    if (object.containsKey("schema"))
                    {
                        String schemaStr = object.getString("schema");
                        String type = resolveType(schemaStr);
                        records.add(new KafkaTopicSchemaRecord(topicName, policies,
                            matcher.reset(topicName.replace("%s.".formatted(config.risingwaveDb()), ""))
                                .replaceAll(match -> match.group(2).toUpperCase()),
                            subject, type, schemaStr));
                    }
                }
            }
        }

        return records;
    }

    public String extractIdentityFieldFromProtobufSchema(
        String schema)
    {
        String identity = null;
        String[] parts = schema.split(";");
        for (String part : parts)
        {
            part = part.trim();
            if (part.contains("message") || part.contains("syntax"))
            {
                continue;
            }
            String[] tokens = part.split("\\s+");
            if (tokens.length >= 2)
            {
                String fieldName = tokens[1];
                if (fieldName.endsWith("_identity"))
                {
                    identity = fieldName;
                    break;
                }
            }
        }
        return identity;
    }

    public String extractIdentityFieldFromSchema(
        String schema) throws JsonProcessingException
    {
        AtomicReference<String> identity = new AtomicReference<>(null);
        ObjectMapper schemaMapper = new ObjectMapper();
        JsonNode schemaObject = schemaMapper.readTree(schema);

        if (schemaObject.has("fields"))
        {
            JsonNode fieldsNode = schemaObject.get("fields");
            StreamSupport.stream(fieldsNode.spliterator(), false)
                .forEach(field ->
                {
                    String fieldName = field.has("type")
                        ? field.get("type").asText()
                        : fieldsNode.fieldNames().next();
                    if (fieldName.endsWith("_identity"))
                    {
                        identity.set(fieldName);
                    }
                });
        }
        else if (schemaObject.has("properties"))
        {
            JsonNode fieldsNode = schemaObject.get("properties");
            fieldsNode.fieldNames().forEachRemaining(fieldName ->
            {
                if (fieldName.endsWith("_identity"))
                {
                    identity.set(fieldName);
                }
            });
        }

        return identity.get();
    }

    private String resolveType(
        String schema) throws JsonProcessingException
    {
        String type = null;
        if (protoMatcher.reset(schema.toLowerCase()).matches())
        {
            type = "protobuf";
        }
        else
        {
            ObjectMapper schemaMapper = new ObjectMapper();
            JsonNode schemaObject = schemaMapper.readTree(schema);
            if (schemaObject.has("type"))
            {
                String schemaType = schemaObject.get("type").asText();
                switch (schemaType)
                {
                case "record":
                case "enum":
                case "fixed":
                    type = "avro";
                    break;
                default:
                    type = "json";
                    break;
                }
            }
        }

        return type;
    }

    private String resolveSchema(
        String subject)
    {
        return webClient.get()
            .uri(URI.create(config.karapaceUrl()).resolve("/subjects/%s/versions/latest".formatted(subject)))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

}
