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

import io.aklivity.zillabase.service.api.gen.internal.helper.ApicurioHelper;
import io.aklivity.zillabase.service.api.gen.internal.helper.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.helper.ZillaConfigHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KafkaConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KeycloakConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

public class PublishConfigServiceTest
{
    @Mock
    private ApiGenConfig config;

    @Mock
    private KafkaConfig kafkaConfig;

    @Mock
    private KafkaTopicSchemaHelper kafkaService;

    @Mock
    private ApicurioHelper apicurioHelper;

    @Mock
    private KeycloakConfig keycloakConfig;

    @Mock
    private ZillaConfigHelper zillaConfigHelper;

    @InjectMocks
    private PublishConfigService publishConfigService;

    @BeforeEach
    public void setUp()
    {
        MockitoAnnotations.openMocks(this);

        when(config.apicurioUrl()).thenReturn("http://localhost:8080");
        when(config.apicurioGroupId()).thenReturn("public");
        when(kafkaConfig.karapaceUrl()).thenReturn("http://localhost:8081");
        when(keycloakConfig.realm()).thenReturn("zillabase");
        when(keycloakConfig.jwksUrl()).thenReturn(
            "http://keycloak.zillabase.dev:8180/realms/%s/protocol/openid-connect/certs");
    }

    @Test
    public void shouldPublishConfigSuccessfully()
    {
        ApiGenEvent event = new ApiGenEvent(ApiGenEventType.HTTP_ASYNC_API_PUBLISHED, "2.8.0", "1.1", null);

        when(zillaConfigHelper.publishConfig(anyString())).thenReturn(true);

        ApiGenEvent result = publishConfigService.publish(event);

        assertNotNull(result);
        assertEquals(ApiGenEventType.ZILLA_CONFIG_PUBLISHED, result.type());
    }
}
