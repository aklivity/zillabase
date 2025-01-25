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

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.AsyncapiSpecRegisterResponse;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;

@Component
public class ApicurioHelper
{
    public static final String KAFKA_ASYNCAPI_ARTIFACT_ID = "kafka-asyncapi";
    public static final String HTTP_ASYNCAPI_ARTIFACT_ID = "http-asyncapi";
    private static final Pattern TOPIC_PATTERN = Pattern.compile("(\\w+)\\.(\\w+)");

    private final Matcher matcher = TOPIC_PATTERN.matcher("");

    private final ApiGenConfig config;
    private final WebClient webClient;
    private final List<String> channels;
    private final List<String> operations;

    public ApicurioHelper(
        ApiGenConfig config,
        WebClient webClient)
    {
        this.config = config;
        this.webClient = webClient;
        this.channels = new ArrayList<>();
        this.operations = new ArrayList<>();
    }

    public String register(
        String id,
        String spec)
    {
        String newVersion = null;

        try
        {
            ResponseEntity<String> httpResponse = webClient.post()
                .uri(URI.create(config.apicurioUrl())
                    .resolve("/apis/registry/v2/groups/default/artifacts"))
                .header("Content-Type", "application/vnd.aai.asyncapi+yaml")
                .header("X-Registry-ArtifactId", id)
                .bodyValue(spec)
                .retrieve()
                .toEntity(String.class)
                .block();

            final String response = httpResponse != null && httpResponse.getStatusCode().value() == 200
                ? httpResponse.getBody()
                : null;

            if (response != null)
            {
                Jsonb jsonb = JsonbBuilder.newBuilder().build();
                AsyncapiSpecRegisterResponse register = jsonb.fromJson(response, AsyncapiSpecRegisterResponse.class);
                newVersion = register.id;
            }
        }
        catch (Exception e)
        {
            // ignore
        }

        return newVersion;
    }

    public String publishSpec(
        String id,
        String spec)
    {
        String newVersion = null;

        ResponseEntity<String> httpResponse = webClient.post()
            .uri(URI.create(config.apicurioUrl())
                .resolve("/apis/registry/v2/groups/%s/artifacts/%s/versions".formatted(config.apicurioGroupId(), id)))
            .header("Content-Type", "application/vnd.aai.asyncapi+yaml")
            .header("X-Registry-ArtifactId", id)
            .bodyValue(spec)
            .retrieve()
            .toEntity(String.class)
            .block();

        final String response = httpResponse != null && httpResponse.getStatusCode().value() == 200
            ? httpResponse.getBody()
            : null;

        if (response != null)
        {
            Jsonb jsonb = JsonbBuilder.newBuilder().build();
            AsyncapiSpecRegisterResponse register = jsonb.fromJson(response, AsyncapiSpecRegisterResponse.class);
            newVersion = register.version;
        }

        return newVersion;
    }

    public String fetchSpec(
        String artifactId,
        String version)
    {
        return webClient.get()
            .uri(URI.create(config.apicurioUrl())
                .resolve("/apis/registry/v2/groups/default/artifacts/%s/versions/%s".formatted(artifactId, version)))
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }

    public List<String> httpOperations(
        String httpSpecVersion) throws JsonProcessingException
    {
        String httpSpec = fetchSpec(HTTP_ASYNCAPI_ARTIFACT_ID, httpSpecVersion);

        operations.clear();

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode yamlRoot = yamlMapper.readTree(httpSpec);

        ObjectMapper jsonMapper = new ObjectMapper();
        String asJsonString = jsonMapper.writeValueAsString(yamlRoot);
        JsonValue jsonValue = Json.createReader(new StringReader(asJsonString)).readValue();

        JsonObject channelsJson = jsonValue.asJsonObject().getJsonObject("channels");
        for (String channel : channelsJson.keySet())
        {
            if (channel.endsWith("_replies_sink"))
            {
                continue;
            }

            String name = matcher.reset(channel).replaceFirst(match -> match.group(2));
            channels.add(name);
        }

        JsonObject operationsMap = jsonValue.asJsonObject().getJsonObject("operations");
        for (Map.Entry<String, JsonValue> operation : operationsMap.entrySet())
        {
            operations.add(operation.getKey());
        }

        return operations;
    }

    public List<String> kafkaChannels(
        String kafkaSpecVersion) throws JsonProcessingException
    {
        String kafkaSpec = fetchSpec(KAFKA_ASYNCAPI_ARTIFACT_ID, kafkaSpecVersion);

        channels.clear();

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        JsonNode yamlRoot = yamlMapper.readTree(kafkaSpec);

        ObjectMapper jsonMapper = new ObjectMapper();
        String asJsonString = jsonMapper.writeValueAsString(yamlRoot);
        JsonValue jsonValue = Json.createReader(new StringReader(asJsonString)).readValue();

        JsonObject channelsJson = jsonValue.asJsonObject().getJsonObject("channels");
        for (String channel : channelsJson.keySet())
        {
            if (channel.endsWith("_replies_sink"))
            {
                continue;
            }

            String name = matcher.reset(channel).replaceFirst(match -> match.group(2));
            channels.add(name);
        }

        return channels;
    }
}
