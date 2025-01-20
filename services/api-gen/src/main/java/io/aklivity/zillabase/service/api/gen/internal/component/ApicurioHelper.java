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

    private final ApiGenConfig config;
    private final WebClient webClient;
    private final List<String> operations;

    public ApicurioHelper(
        ApiGenConfig config,
        WebClient webClient)
    {
        this.config = config;
        this.webClient = webClient;
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
                .uri(URI.create(config.adminHttpUrl())
                    .resolve("/v1/asyncapis"))
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
            .uri(URI.create(config.adminHttpUrl())
                .resolve("/v1/asyncapis/artifacts/%s".formatted(id)))
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
            .uri(URI.create(config.adminHttpUrl())
                .resolve("/v1/asyncapis/%s/versions/%s".formatted(artifactId, version)))
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
        JsonObject operationsMap = jsonValue.asJsonObject().getJsonObject("operations");
        for (Map.Entry<String, JsonValue> operation : operationsMap.entrySet())
        {
            this.operations.add(operation.getKey());
        }

        return operations;
    }

    public boolean publishConfig(
        String zillaConfig)
    {
        ResponseEntity<Void> response = webClient
            .put()
            .uri(URI.create(config.adminHttpUrl()).resolve("/v1/config/zilla.yaml"))
            .bodyValue(zillaConfig)
            .retrieve()
            .toBodilessEntity()
            .block();

        return response != null && response.getStatusCode().value() == 204;
    }
}
