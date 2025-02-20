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
package io.aklivity.zillabase.service.api.gen.internal.generator;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.aklivity.zillabase.service.api.gen.internal.helper.ApicurioHelper.HTTP_ASYNCAPI_ARTIFACT_ID;
import static io.aklivity.zillabase.service.api.gen.internal.helper.ApicurioHelper.KAFKA_ASYNCAPI_ARTIFACT_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaAsyncApiConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaAsyncApiConfigBuilder;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaBindingConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaBindingConfigBuilder;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaBindingOptionsConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaBindingRouteConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaCatalogConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaGuardConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KafkaConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KeycloakConfig;
import io.aklivity.zillabase.service.api.gen.internal.helper.KafkaTopicSchemaHelper;

@Component
public class ZillaConfigGenerator
{
    private static final String AUTH_JWT = "jwt0";

    private final List<String> suffixes = Arrays.asList("ReadItem", "Update", "Read", "Create", "Delete",
            "Get", "GetItem");
    private final Map<String, Map<String, Map<String, String>>> httpApi = Map.of(
            "catalog", Map.of("apicurio_catalog", Map.of("subject", HTTP_ASYNCAPI_ARTIFACT_ID,
                "version", "latest")));
    private final Map<String, Map<String, Map<String, String>>> kafkaApi = Map.of(
        "catalog", Map.of("apicurio_catalog", Map.of("subject", KAFKA_ASYNCAPI_ARTIFACT_ID,
            "version", "latest")));

    private final ApiGenConfig config;
    private final KeycloakConfig keycloakConfig;
    private final KafkaConfig kafkaConfig;
    private final KafkaTopicSchemaHelper kafkaService;

    public ZillaConfigGenerator(
        ApiGenConfig config,
        KeycloakConfig keycloakConfig,
        KafkaConfig kafkaConfig,
        KafkaTopicSchemaHelper kafkaService)
    {
        this.config = config;
        this.keycloakConfig = keycloakConfig;
        this.kafkaConfig = kafkaConfig;
        this.kafkaService = kafkaService;
    }

    public String generate(
        List<String> operations)
    {
        String zillaConfig = null;
        try
        {
            ZillaAsyncApiConfig zilla = ZillaAsyncApiConfig.builder()
                .name("zilla-http-kafka-asyncapi")
                .inject(this::injectCatalog)
                .inject(this::injectGuard)
                .inject(this::injectTelemetry)
                .inject(config -> injectBindings(config, operations))
                .build();

            ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
                .setSerializationInclusion(NON_NULL);

            zillaConfig = mapper.writeValueAsString(zilla);
            zillaConfig = zillaConfig.replaceAll("(:status|zilla:correlation-id)", "'$1'");
        }
        catch (Exception e)
        {
            System.out.println("Failed to generate zilla config: " + e.getMessage());
        }

        return zillaConfig;
    }

    private <C> ZillaAsyncApiConfigBuilder<C> injectCatalog(
        ZillaAsyncApiConfigBuilder<C> builder)
    {
        ZillaCatalogConfig apicurioCatalog = new ZillaCatalogConfig();
        apicurioCatalog.type = "apicurio";
        apicurioCatalog.options = Map.of(
            "url", config.apicurioUrl(),
            "group-id", config.apicurioGroupId());

        ZillaCatalogConfig karapaceCatalog = new ZillaCatalogConfig();
        karapaceCatalog.type = "karapace";
        karapaceCatalog.options = Map.of("url", kafkaConfig.karapaceUrl());

        builder.catalogs(Map.of(
            "apicurio_catalog", apicurioCatalog,
            "karapace_catalog", karapaceCatalog));

        return builder;
    }

    private <C> ZillaAsyncApiConfigBuilder<C> injectGuard(
        ZillaAsyncApiConfigBuilder<C> builder)
    {
        String realm = keycloakConfig.realm();

        if (realm != null)
        {
            ZillaGuardConfig guard = new ZillaGuardConfig();
            guard.type = "jwt";
            guard.options.issuer = "%s/realms/%s".formatted(keycloakConfig.issuer(), realm);
            guard.options.audience = keycloakConfig.audience();
            guard.options.keys = keycloakConfig.jwksUrl().formatted(realm);
            guard.options.identity = "preferred_username";

            builder.guards(Map.of(AUTH_JWT, guard));
        }

        return builder;
    }

    private <C> ZillaAsyncApiConfigBuilder<C> injectTelemetry(
        ZillaAsyncApiConfigBuilder<C> builder)
    {
        builder.telemetry(Map.of("exporters",
            Map.of("stdout_logs_exporter", Map.of("type", "stdout"))));

        return builder;
    }

    private <C> ZillaAsyncApiConfigBuilder<C> injectBindings(
        ZillaAsyncApiConfigBuilder<C> builder,
        List<String> operations)
    {
        builder
            .inject(this::injectNorthHttpServerBinding)
            .inject(b -> injectSouthKafkaProxyBinding(b, operations))
            .inject(this::injectSouthKafkaClientBinding);

        return builder;
    }

    private <C> ZillaAsyncApiConfigBuilder<C> injectNorthHttpServerBinding(
        ZillaAsyncApiConfigBuilder<C> builder)
    {
        ZillaBindingConfig northHttpServer = ZillaBindingConfig.builder()
            .type("asyncapi")
            .kind("server")
            .inject(this::injectNorthHttpServerOption)
            .exit("south_kafka_proxy")
            .build();

        builder.addBinding("north_http_server", northHttpServer);

        return builder;
    }

    private <C> ZillaBindingConfigBuilder<C> injectNorthHttpServerOption(
        ZillaBindingConfigBuilder<C> builder)
    {
        ZillaBindingOptionsConfig optionsConfig = new ZillaBindingOptionsConfig();
        optionsConfig.specs = Map.of("http_api", httpApi);

        String realm = keycloakConfig.realm();

        if (realm != null)
        {
            optionsConfig.http = new ZillaBindingOptionsConfig.HttpAuthorizationOptionsConfig();
            optionsConfig.http.authorization = Map.of(AUTH_JWT,
                Map.of("credentials",
                    Map.of("headers",
                        Map.of("authorization", "Bearer {credentials}"),
                        "query", Map.of("access_token", "{credentials}"))));
        }

        builder.options(optionsConfig);

        return builder;
    }

    private <C> ZillaAsyncApiConfigBuilder<C> injectSouthKafkaProxyBinding(
        ZillaAsyncApiConfigBuilder<C> builder,
        List<String> operations)
    {
        ZillaBindingConfig southKafkaProxy = ZillaBindingConfig.builder()
            .type("asyncapi")
            .kind("proxy")
            .inject(this::injectSouthKafkaProxyOption)
            .inject(b -> injectSouthKafkaProxyRoutes(b, operations))
            .build();

        builder.addBinding("south_kafka_proxy", southKafkaProxy);

        return builder;
    }

    private <C> ZillaBindingConfigBuilder<C> injectSouthKafkaProxyOption(
        ZillaBindingConfigBuilder<C> builder)
    {
        List<ZillaBindingOptionsConfig.KafkaTopicConfig> topicsConfig = new ArrayList<>();
        extractedHeaders(topicsConfig);

        ZillaBindingOptionsConfig.KafkaOptionsConfig kafkaOptionsConfig =
            new ZillaBindingOptionsConfig.KafkaOptionsConfig();
        kafkaOptionsConfig.topics = topicsConfig;

        ZillaBindingOptionsConfig optionsConfig = new ZillaBindingOptionsConfig();
        optionsConfig.specs = Map.of("http_api", httpApi, "kafka_api", kafkaApi);

        builder.options(optionsConfig);
        optionsConfig.kafka = kafkaOptionsConfig;

        builder.options(optionsConfig);

        return builder;
    }

    private <C> ZillaBindingConfigBuilder<C> injectSouthKafkaProxyRoutes(
        ZillaBindingConfigBuilder<C> builder,
        List<String> operations)
    {
        operations.stream()
            .filter(o -> !o.endsWith("Replies"))
            .forEach(o ->
            {
                ZillaBindingRouteConfig route = ZillaBindingRouteConfig.builder()
                    .when(List.of(Map.of("api-id", "http_api", "operation-id", o)))
                    .with(Map.of("api-id", "kafka_api", "operation-id", suffixes.stream()
                        .filter(o::endsWith)
                        .map(suffix -> o.substring(0, o.length() - suffix.length()))
                        .findFirst()
                        .orElse(o)))
                    .exit("south_kafka_client")
                    .build();

                builder.addRoute(route);
            });

        return builder;
    }

    private <C> ZillaAsyncApiConfigBuilder<C> injectSouthKafkaClientBinding(
        ZillaAsyncApiConfigBuilder<C> builder)
    {
        ZillaBindingConfig southKafkaClient = ZillaBindingConfig.builder()
            .type("asyncapi")
            .kind("client")
            .inject(this::injectSouthKafkaClientOption)
            .build();

        builder.addBinding("south_kafka_client", southKafkaClient);

        return builder;
    }

    private <C> ZillaBindingConfigBuilder<C> injectSouthKafkaClientOption(
        ZillaBindingConfigBuilder<C> builder)
    {
        ZillaBindingOptionsConfig optionsConfig = new ZillaBindingOptionsConfig();
        optionsConfig.specs = Map.of("kafka_api", kafkaApi);

        builder.options(optionsConfig);

        return builder;
    }

    private void extractedHeaders(
        List<ZillaBindingOptionsConfig.KafkaTopicConfig> topicsConfig)
    {
        try
        {
            kafkaService.resolve()
                .forEach(record ->
                {
                    ZillaBindingOptionsConfig.KafkaTopicConfig topicConfig =
                        new ZillaBindingOptionsConfig.KafkaTopicConfig();

                    topicConfig.name = record.name;

                    if (record.name.endsWith("_replies"))
                    {
                        extractRepliesHeader(topicConfig, record);
                    }
                    else
                    {
                        extractHeader(topicConfig, record);
                    }

                    topicsConfig.add(topicConfig);
                });
        }
        catch (Exception e)
        {
            System.out.println("Failed to resolve Kafka topics");
        }
    }

    private void extractHeader(
        ZillaBindingOptionsConfig.KafkaTopicConfig topicConfig,
        KafkaTopicSchemaRecord record)
    {
        String identity = record.type.equals("protobuf")
            ? kafkaService.findIdentityFieldFromProtobuf(record.schema)
            : kafkaService.findIdentityField(record.schema);

        if (identity != null)
        {
            ZillaBindingOptionsConfig.ModelConfig value = new ZillaBindingOptionsConfig.ModelConfig();
            value.model = record.type;
            value.catalog = Map.of("catalog0",
                List.of(Map.of("subject", record.subject, "version", "latest")));

            ZillaBindingOptionsConfig.TransformConfig transforms =
                new ZillaBindingOptionsConfig.TransformConfig();
            transforms.headers = Map.of("identity", "${message.value.%s}".formatted(identity));
            topicConfig.value = value;
            topicConfig.transforms = List.of(transforms);
        }
    }

    private void extractRepliesHeader(
        ZillaBindingOptionsConfig.KafkaTopicConfig topicConfig,
        KafkaTopicSchemaRecord record)
    {
        ZillaBindingOptionsConfig.ModelConfig value = new ZillaBindingOptionsConfig.ModelConfig();
        value.model = record.type;
        value.catalog = Map.of("catalog0",
            List.of(Map.of("subject", record.subject, "version", "latest")));

        ZillaBindingOptionsConfig.TransformConfig transforms =
            new ZillaBindingOptionsConfig.TransformConfig();
        transforms.headers = Map.of(":status", "${message.value.status}",
            "zilla:correlation-id", "${message.value.correlation_id}");
        topicConfig.value = value;
        topicConfig.transforms = List.of(transforms);
    }
}
