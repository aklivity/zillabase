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
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.aklivity.zillabase.service.api.gen.internal.component.ApicurioHelper;
import io.aklivity.zillabase.service.api.gen.internal.component.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

public class HttpAsyncApiServiceTest
{
    @Mock
    private ApiGenConfig config;

    @Mock
    private ApicurioHelper specHelper;

    @Mock
    private KafkaTopicSchemaHelper kafkaHelper;

    @InjectMocks
    private HttpAsyncApiService httpAsyncApiService;

    private String kafkaSpec;

    @BeforeEach
    public void setUp() throws URISyntaxException, IOException
    {
        MockitoAnnotations.openMocks(this);

        kafkaSpec = new String(Files.readAllBytes(Paths.get(Objects.requireNonNull(
            getClass().getClassLoader().getResource("specs/kafka-asyncapi.yaml")).toURI())), UTF_8);
    }

    @Test
    public void shouldGenerateHttpAsyncApiEvent()
    {
        String kafkaSpecVersion = "1";
        String httpSpecVersion = "1";
        ApiGenEvent inputEvent = new ApiGenEvent(ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED, kafkaSpecVersion, null, null);

        when(specHelper.fetchSpec(KAFKA_ASYNCAPI_ARTIFACT_ID, kafkaSpecVersion)).thenReturn(kafkaSpec);
        when(specHelper.publishSpec(anyString(), anyString())).thenReturn(httpSpecVersion);

        ApiGenEvent resultEvent = httpAsyncApiService.generate(inputEvent);

        assertEquals(ApiGenEventType.HTTP_ASYNC_API_PUBLISHED, resultEvent.type());
        assertEquals("1", resultEvent.kafkaVersion());
        assertEquals(httpSpecVersion, resultEvent.httpVersion());

        verify(specHelper, times(1)).fetchSpec(KAFKA_ASYNCAPI_ARTIFACT_ID, kafkaSpecVersion);
        verify(specHelper, times(1)).publishSpec(anyString(), anyString());
    }

    @Test
    public void shouldHandleExceptionDuringGeneration()
    {
        ApiGenEvent inputEvent = new ApiGenEvent(ApiGenEventType.KAFKA_ASYNC_API_PUBLISHED, "kafkaVersion", null, null);

        when(specHelper.fetchSpec(anyString(), anyString())).thenThrow(new RuntimeException("Error"));

        ApiGenEvent resultEvent = httpAsyncApiService.generate(inputEvent);

        assertEquals(ApiGenEventType.HTTP_ASYNC_API_ERRORED, resultEvent.type());
        assertEquals("kafkaVersion", resultEvent.kafkaVersion());
        assertEquals(null, resultEvent.httpVersion());

        verify(specHelper, times(1)).fetchSpec(anyString(), anyString());
        verify(specHelper, never()).publishSpec(anyString(), anyString());
    }
}
