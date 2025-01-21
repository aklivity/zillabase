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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

public class PublishConfigServiceTest
{
    @Mock
    private ApiGenConfig config;

    @Mock
    private KafkaTopicSchemaHelper kafkaService;

    @Mock
    private ApicurioHelper specService;

    @InjectMocks
    private PublishConfigService publishConfigService;

    @BeforeEach
    public void setUp()
    {
        MockitoAnnotations.openMocks(this);

        when(config.apicurioUrl()).thenReturn("http://localhost:8080");
        when(config.apicurioGroupId()).thenReturn("public");
        when(config.karapaceUrl()).thenReturn("http://localhost:8081");
    }

    @Test
    public void shouldPublishConfigSuccessfully()
    {
        ApiGenEvent event = new ApiGenEvent(ApiGenEventType.HTTP_ASYNC_API_PUBLISHED, "2.8.0", "1.1", null);

        when(specService.publishConfig(anyString())).thenReturn(true);

        ApiGenEvent result = publishConfigService.publish(event);

        assertNotNull(result);
        assertEquals(ApiGenEventType.ZILLA_CONFIG_PUBLISHED, result.type());
    }
}
