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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.aklivity.zillabase.service.api.gen.internal.asyncapi.KafkaTopicSchemaRecord;
import io.aklivity.zillabase.service.api.gen.internal.asyncapi.zilla.ZillaBindingOptionsConfig;
import io.aklivity.zillabase.service.api.gen.internal.generator.ZillaConfigGenerator;
import io.aklivity.zillabase.service.api.gen.internal.helper.ApicurioHelper;
import io.aklivity.zillabase.service.api.gen.internal.helper.KafkaTopicSchemaHelper;
import io.aklivity.zillabase.service.api.gen.internal.helper.KeycloakHelper;
import io.aklivity.zillabase.service.api.gen.internal.helper.ZillaConfigHelper;
import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KafkaConfig;
import io.aklivity.zillabase.service.api.gen.internal.config.KeycloakConfig;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEvent;
import io.aklivity.zillabase.service.api.gen.internal.model.ApiGenEventType;

@Service
public class PublishConfigService
{
    private final ZillaConfigGenerator configGenerator;
    private final ApicurioHelper specHelper;
    private final ZillaConfigHelper zillaConfigHelper;
    private final KeycloakHelper keycloakHelper;

    public PublishConfigService(
        ZillaConfigGenerator configGenerator,
        ApicurioHelper specHelper,
        ZillaConfigHelper zillaConfigHelper,
        KeycloakHelper keycloakHelper)
    {
        this.configGenerator = configGenerator;
        this.specHelper = specHelper;
        this.zillaConfigHelper = zillaConfigHelper;
        this.keycloakHelper = keycloakHelper;
    }

    public ApiGenEvent publish(
        ApiGenEvent event)
    {
        ApiGenEventType newState;
        String message = null;

        try
        {
            List<String> operations = specHelper.httpOperations(event.httpVersion());
            List<String> channels = specHelper.kafkaChannels(event.httpVersion());
            String zillaConfig = configGenerator.generate(operations);
            boolean published = zillaConfigHelper.publishConfig(zillaConfig);

            if (published)
            {
                createAndAssignScope(channels);
            }

            newState = published ? ApiGenEventType.ZILLA_CONFIG_PUBLISHED : ApiGenEventType.ZILLA_CONFIG_ERRORED;
        }
        catch (Exception ex)
        {
            newState = ApiGenEventType.ZILLA_CONFIG_ERRORED;
            message = ex.getMessage();
            ex.printStackTrace(System.err);
        }

        return new ApiGenEvent(newState, event.kafkaVersion(), event.httpVersion(), message);
    }


    private void createAndAssignScope(
        List<String> channels)
    {
        for (String channel : channels)
        {
            keycloakHelper.createAndAssignScope("%s:read".formatted(channel));
            keycloakHelper.createAndAssignScope("%s:write".formatted(channel));
        }
    }
}
