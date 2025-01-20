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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

public class KafkaAsyncApiServiceTest
{
    @Mock
    private ApiGenConfig config;

    @Mock
    private KafkaTopicSchemaHelper kafkaHelper;

    @Mock
    private ApicurioHelper specHelper;

    @InjectMocks
    private KafkaAsyncApiService kafkaAsyncApiService;

    @BeforeEach
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(config.kafkaBootstrapServers()).thenReturn("localhost:9092");
        when(config.risingwaveDb()).thenReturn("dev");
    }

    @Test
    public void shouldGenerateKafkaAsyncApiEvent() throws Exception
    {
        List<KafkaTopicSchemaRecord> schemaRecords = List.of(
            new KafkaTopicSchemaRecord(
                "test-topic",
                new String[]{"delete"},
                "TestTopic",
                "test-topic-value",
                "record",
                "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}," +
                    " {\"name\":\"name\",\"type\":\"string\"}]}")
        );

        String specVersion = "1";

        when(kafkaHelper.resolve()).thenReturn(schemaRecords);
        when(specHelper.register(anyString(), anyString())).thenReturn(specVersion);

        ApiGenEvent inputEvent = new ApiGenEvent(ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED, null, null);

        ApiGenEvent resultEvent = kafkaAsyncApiService.generate(inputEvent);

        assertEquals(ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED, resultEvent.type());
        assertEquals(specVersion, resultEvent.kafkaVersion());

        verify(kafkaHelper, times(1)).resolve();
        verify(specHelper, times(1)).register(anyString(), anyString());
    }
}
