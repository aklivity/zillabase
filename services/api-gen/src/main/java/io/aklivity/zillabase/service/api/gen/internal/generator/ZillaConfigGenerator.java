package io.aklivity.zillabase.service.api.gen.internal.generator;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static io.aklivity.zillabase.service.api.gen.internal.helper.ApicurioHelper.HTTP_ASYNCAPI_ARTIFACT_ID;
import static io.aklivity.zillabase.service.api.gen.internal.helper.ApicurioHelper.KAFKA_ASYNCAPI_ARTIFACT_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.springframework.stereotype.Component;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaAsyncApiConfig;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaBindingConfig;
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
            karapaceCatalog.options = Map.of("url", kafkaConfig.karapaceUrl());

            String realm = keycloakConfig.realm();
            String authnJwt = "jwt0";
            if (realm != null)
            {
                ZillaGuardConfig guard = new ZillaGuardConfig();
                guard.type = "jwt";
                guard.options.issuer = "%s/realms/%s".formatted(keycloakConfig.issuer(), realm);
                guard.options.audience = keycloakConfig.audience();
                guard.options.keys = keycloakConfig.jwksUrl().formatted(realm);
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

            zillaConfig = mapper.writeValueAsString(zilla);
            zillaConfig = zillaConfig.replaceAll("(:status|zilla:correlation-id)", "'$1'");
        }
        catch (Exception e)
        {
            System.out.println("Failed to generate zilla config: " + e.getMessage());
        }

        return zillaConfig;
    }

    private void extractedHeaders(
        List<ZillaBindingOptionsConfig.KafkaTopicConfig> topicsConfig)
    {
        List<KafkaTopicSchemaRecord> records = null;
        try
        {
            records = kafkaService.resolve();

            for (KafkaTopicSchemaRecord record : records)
            {
                if (record.name.endsWith("_replies"))
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
                        ? kafkaService.findIdentityFieldFromProtobuf(record.schema)
                        : kafkaService.findIdentityField(record.schema);
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
        catch (Exception e)
        {
            System.out.println("Failed to resolve Kafka topics");
        }

    }
}
