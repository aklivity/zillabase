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
package io.aklivity.zillabase.service.api.gen.internal.helper;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.aklivity.zillabase.service.api.gen.internal.config.ApiGenConfig;

@Component
public class ZillaConfigHelper
{
    private final ApiGenConfig config;
    private final WebClient webClient;

    public ZillaConfigHelper(
        ApiGenConfig config,
        WebClient webClient)
    {
        this.config = config;
        this.webClient = webClient;
    }

    public boolean publishConfig(
        String zillaConfig)
    {
        ResponseEntity<Void> response = webClient
            .put()
            .uri(URI.create(config.configServerUrl()).resolve("/config/zilla.yaml"))
            .bodyValue(zillaConfig)
            .retrieve()
            .toBodilessEntity()
            .block();

        return response != null && response.getStatusCode().value() == 204;
    }
}
