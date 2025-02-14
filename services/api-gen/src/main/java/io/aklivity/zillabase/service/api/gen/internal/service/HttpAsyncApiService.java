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

import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.HTTP_ASYNCAPI_ARTIFACT_ID;
import static io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper.KAFKA_ASYNCAPI_ARTIFACT_ID;

import org.springframework.stereotype.Service;

import io.aklivity.zillabase.service.api.gen.internal.builder.HttpAsyncApiBuilder;
import io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

@Service
public class HttpAsyncApiService
{
    private final KafkaTopicSchemaHelper kafkaHelper;
    private final ApicurioHelper specHelper;

    public HttpAsyncApiService(
        ApicurioHelper specHelper,
        KafkaTopicSchemaHelper kafkaHelper)
    {
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

            HttpAsyncApiBuilder builder = new HttpAsyncApiBuilder(kafkaHelper);
            String httpSpec = builder.buildSpec(kafkaSpec);

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
