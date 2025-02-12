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

import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.KAFKA_ASYNCAPI_ARTIFACT_ID;

import java.util.List;

import org.springframework.stereotype.Service;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.builder.KafkaAsyncApiBuilder;
import io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KafkaConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

@Service
public class KafkaAsyncApiService
{
    private final ApiGenConfig config;
    private final KafkaConfig
        kafkaConfig;
    private final KafkaTopicSchemaHelper kafkaHelper;
    private final ApicurioHelper specHelper;

    public KafkaAsyncApiService(
        ApiGenConfig config,
        KafkaConfig kafkaConfig,
        KafkaTopicSchemaHelper kafkaHelper,
        ApicurioHelper specHelper)
    {
        this.config = config;
        this.kafkaConfig = kafkaConfig;
        this.kafkaHelper = kafkaHelper;
        this.specHelper = specHelper;
    }

    public ApiGenEvent generate(
        ApiGenEvent event)
    {
        ApiGenEventType eventType;
        String specVersion = null;
        String message = null;

        try
        {
            List<KafkaTopicSchemaRecord> schemaRecords = kafkaHelper.resolve();

            KafkaAsyncApiBuilder builder = new KafkaAsyncApiBuilder(config, kafkaConfig);
            String kafkaSpec = builder.buildSpec(schemaRecords);

            if (kafkaSpec != null)
            {
                specVersion = specHelper.publishSpec(KAFKA_ASYNCAPI_ARTIFACT_ID, kafkaSpec);
                eventType = ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED;
            }
            else
            {
                eventType = ApiGenEventType.KAFKA_ASYNC_API_ERRORED;
            }
        }
        catch (Exception ex)
        {
            eventType = ApiGenEventType.KAFKA_ASYNC_API_ERRORED;
            message = ex.getMessage();
        }

        return new ApiGenEvent(eventType, specVersion, null, message);
    }
}
