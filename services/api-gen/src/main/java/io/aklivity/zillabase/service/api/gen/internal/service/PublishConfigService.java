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
import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.HTTP_ASYNCAPI_ARTIFACT_ID;
import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.KAFKA_ASYNCAPI_ARTIFACT_ID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaAsyncApiConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaBindingConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaBindingOptionsConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaBindingRouteConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaCatalogConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaGuardConfig;
import io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

@Service
public class PublishConfigService
{
    private final ApiGenConfig config;
    private final KafkaTopicSchemaHelper kafkaService;
    private final ApicurioHelper specService;

    private final List<KafkaTopicSchemaRecord> records;

    public PublishConfigService(
        ApiGenConfig config,
        KafkaTopicSchemaHelper kafkaService,
        ApicurioHelper specService)
    {
        this.config = config;
        this.kafkaService = kafkaService;
        this.specService = specService;

        this.records = new ArrayList<>();
    }

    public ApiGenEvent publish(
        ApiGenEvent event)
    {
        ApiGenEventType newState;

        try
        {
            String zillaConfig = generateConfig(event);
            boolean published = specService.publishConfig(zillaConfig);

            newState = published ? ApiGenEventType.ZILLA_CONFIG_PUBLISHED : ApiGenEventType.ZILL_CONFIG_ERRORED;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            newState = ApiGenEventType.ZILL_CONFIG_ERRORED;
        }

        return new ApiGenEvent(newState, event.kafkaVersion(), event.httpVersion());
    }

    private String generateConfig(
        ApiGenEvent event) throws IOException
    {
        List<String> suffixes = Arrays.asList("ReadItem", "Update", "Read", "Create", "Delete",
                    "Get", "GetItem");

        ZillaAsyncApiConfig zilla = new ZillaAsyncApiConfig();
        ZillaCatalogConfig apicurioCatalog = new ZillaCatalogConfig();
        apicurioCatalog.type = "apicurio";
        apicurioCatalog.options = Map.of(
            "url", config.apicurioUrl(),
            "group-id", config.apicurioGroupId());

        ZillaCatalogConfig karapaceCatalog = new ZillaCatalogConfig();
        karapaceCatalog.type = "karapace";
        karapaceCatalog.options = Map.of("url", config.karapaceUrl());

        String realm = config.keycloakRealm();
        String authnJwt = "jwt0";
        if (realm != null)
        {
            ZillaGuardConfig guard = new ZillaGuardConfig();
            guard.type = "jwt";
            guard.options.issuer = "%s/realms/%s".formatted(config.keycloakUrl(), realm);
            guard.options.audience = config.keycloakAudience();
            guard.options.keys = config.keycloakJwksUrl().formatted(realm);
            guard.options.identity = "preferred_username";

            zilla.guards = Map.of(authnJwt, guard);
        }

        Map<String, Map<String, Map<String, String>>> httpApi = Map.of(
            "catalog", Map.of("apicurio_catalog", Map.of("subject", HTTP_ASYNCAPI_ARTIFACT_ID,
                "version", "latest")));
        Map<String, Map<String, Map<String, String>>> kafkaApi = Map.of(
            "catalog", Map.of("apicurio_catalog", Map.of("subject", KAFKA_ASYNCAPI_ARTIFACT_ID,
                "version", "latest")));

        List<ZillaBindingRouteConfig> routes = new ArrayList<>();

        List<String> operations = specService.httpOperations(event.httpVersion());

        for (String operation : operations)
        {
            if (operation.endsWith("Replies"))
            {
                continue;
            }
            ZillaBindingRouteConfig route = new ZillaBindingRouteConfig();
            route.when = List.of(Map.of("api-id", "http_api", "operation-id", operation));
            route.with = Map.of("api-id", "kafka_api", "operation-id", suffixes.stream()
                .filter(operation::endsWith)
                .map(suffix -> operation.substring(0, operation.length() - suffix.length()))
                .findFirst()
                .orElse(operation));
            route.exit = "south_kafka_client";
            routes.add(route);
        }

        Map<String, ZillaBindingConfig> bindings = new HashMap<>();

        ZillaBindingConfig northHttpServer = new ZillaBindingConfig();
        northHttpServer.type = "asyncapi";
        northHttpServer.kind = "server";
        ZillaBindingOptionsConfig optionsConfig = new ZillaBindingOptionsConfig();
        optionsConfig.specs = Map.of("http_api", httpApi);
        if (realm != null)
        {
            optionsConfig.http = new ZillaBindingOptionsConfig.HttpAuthorizationOptionsConfig();
            optionsConfig.http.authorization = Map.of(authnJwt,
                Map.of("credentials",
                    Map.of("headers",
                        Map.of("authorization", "Bearer {credentials}"),
                        "query", Map.of("access_token", "{credentials}"))));
        }
        northHttpServer.options = optionsConfig;
        northHttpServer.exit = "south_kafka_proxy";
        bindings.put("north_http_server", northHttpServer);

        ZillaBindingConfig southKafkaProxy = new ZillaBindingConfig();
        southKafkaProxy.type = "asyncapi";
        southKafkaProxy.kind = "proxy";
        optionsConfig = new ZillaBindingOptionsConfig();
        optionsConfig.specs = Map.of("http_api", httpApi, "kafka_api", kafkaApi);
        southKafkaProxy.options = optionsConfig;
        southKafkaProxy.routes = routes;
        bindings.put("south_kafka_proxy", southKafkaProxy);

        ZillaBindingConfig southKafkaClient = new ZillaBindingConfig();
        southKafkaClient.type = "asyncapi";
        southKafkaClient.kind = "client";
        optionsConfig = new ZillaBindingOptionsConfig();
        optionsConfig.specs = Map.of("kafka_api", kafkaApi);
        List<ZillaBindingOptionsConfig.KafkaTopicConfig> topicsConfig = new ArrayList<>();
        extractedHeaders(topicsConfig);

        ZillaBindingOptionsConfig.KafkaOptionsConfig kafkaOptionsConfig =
            new ZillaBindingOptionsConfig.KafkaOptionsConfig();
        kafkaOptionsConfig.topics = topicsConfig;
        optionsConfig.kafka = kafkaOptionsConfig;
        southKafkaClient.options = optionsConfig;
        bindings.put("south_kafka_client", southKafkaClient);

        zilla.name = "zilla-http-kafka-asyncapi";
        zilla.catalogs = Map.of(
            "apicurio_catalog", apicurioCatalog,
            "karapace_catalog", karapaceCatalog);
        zilla.bindings = bindings;
        zilla.telemetry = Map.of("exporters", Map.of("stdout_logs_exporter", Map.of("type", "stdout")));

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
            .setSerializationInclusion(NON_NULL);

        String zillaConfig = mapper.writeValueAsString(zilla);
        zillaConfig = zillaConfig.replaceAll("(:status|zilla:correlation-id)", "'$1'");

        return zillaConfig;
    }

    private void extractedHeaders(
        List<ZillaBindingOptionsConfig.KafkaTopicConfig> topicsConfig) throws JsonProcessingException
    {
        for (KafkaTopicSchemaRecord record : records)
        {
            if (record.name.endsWith("_replies_sink"))
            {
                ZillaBindingOptionsConfig.KafkaTopicConfig topicConfig =
                    new ZillaBindingOptionsConfig.KafkaTopicConfig();
                topicConfig.name = record.name;
                ZillaBindingOptionsConfig.TransformConfig transforms =
                    new ZillaBindingOptionsConfig.TransformConfig();
                transforms.headers = Map.of(":status", "${message.value.status}",
                    "zilla:correlation-id", "${message.value.correlation_id}");
                ZillaBindingOptionsConfig.ModelConfig value = new ZillaBindingOptionsConfig.ModelConfig();
                value.model = record.type;
                value.catalog = Map.of("catalog0",
                    List.of(Map.of("subject", record.subject, "version", "latest")));
                topicConfig.value = value;
                topicConfig.transforms = List.of(transforms);
                topicsConfig.add(topicConfig);
            }
            else
            {
                String identity = record.type.equals("protobuf")
                    ? kafkaService.extractIdentityFieldFromProtobufSchema(record.schema)
                    : kafkaService.extractIdentityFieldFromSchema(record.schema);
                if (identity != null)
                {
                    ZillaBindingOptionsConfig.KafkaTopicConfig topicConfig =
                        new ZillaBindingOptionsConfig.KafkaTopicConfig();
                    topicConfig.name = record.name;
                    ZillaBindingOptionsConfig.ModelConfig value = new ZillaBindingOptionsConfig.ModelConfig();
                    value.model = record.type;
                    value.catalog = Map.of("catalog0",
                        List.of(Map.of("subject", record.subject, "version", "latest")));
                    topicConfig.value = value;
                    ZillaBindingOptionsConfig.TransformConfig transforms =
                        new ZillaBindingOptionsConfig.TransformConfig();
                    transforms.headers = Map.of("identity", "${message.value.%s}".formatted(identity));
                    topicConfig.transforms = List.of(transforms);
                    topicsConfig.add(topicConfig);
                }
            }
        }
    }
}
