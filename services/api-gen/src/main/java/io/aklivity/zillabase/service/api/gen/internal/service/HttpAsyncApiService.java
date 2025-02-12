package io.aklivity.zillabase.service.api.gen.internal.service;

import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.HTTP_ASYNCAPI_ARTIFACT_ID;
import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.KAFKA_ASYNCAPI_ARTIFACT_ID;

import org.springframework.stereotype.Service;

import io.aklivity.zillabase.service.api.gen.internal.spec.HttpAsyncApiBuilder;
import io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

@Service
public class HttpAsyncApiService
{
    private final ApiGenConfig config;
    private final KafkaTopicSchemaHelper kafkaHelper;
    private final ApicurioHelper specHelper;

    public HttpAsyncApiService(
        ApiGenConfig config,
        ApicurioHelper specHelper,
        KafkaTopicSchemaHelper kafkaHelper)
    {
        this.config = config;
        this.kafkaHelper = kafkaHelper;
        this.specHelper = specHelper;
    }

    public ApiGenEvent generate(
        ApiGenEvent event)
    {
        ApiGenEventType eventType;
        String httpSpecVersion = null;
        String message = null;

        try
        {
            String kafkaSpec = specHelper.fetchSpec(KAFKA_ASYNCAPI_ARTIFACT_ID, event.kafkaVersion());

            HttpAsyncApiBuilder builder = new HttpAsyncApiBuilder(config, kafkaHelper);
            String httpSpec = builder.buildYamlSpec(kafkaSpec);

            httpSpecVersion = specHelper.publishSpec(HTTP_ASYNCAPI_ARTIFACT_ID, httpSpec);

            eventType = ApiGenEventType.HTTP_ASYNC_API_PUBLISHED;
        }
        catch (Exception ex)
        {
            eventType = ApiGenEventType.HTTP_ASYNC_API_ERRORED;
            message = ex.getMessage();
        }

        return new ApiGenEvent(eventType, event.kafkaVersion(), httpSpecVersion, message);
    }
}
