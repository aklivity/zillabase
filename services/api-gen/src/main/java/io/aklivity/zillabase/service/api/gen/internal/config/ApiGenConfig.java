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
package io.aklivity.zillabase.service.api.gen.internal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.gen")
public class ApiGenConfig
{
    @Value("${admin.http.port:7184}")
    private int adminHttpPort;

    @Value("${risingwave.db:dev}")
    private String risingwaveDb;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String kafkaBootstrapServers;

    @Value("${KARAPACE_URL:http://karapace.zillabase.dev:8081}")
    private String karapaceUrl;

    @Value("${apicurio.registry.url}")
    private String apicurioUrl;

    @Value("${apicurio.group.id}")
    private String apicurioGroupId;

    @Value("${keycloak.url:http://localhost:8180}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.audience:account}")
    private String keycloakAudience;

    @Value("${keycloak.jwks.url:http://keycloak.zillabase.dev:8180/realms/%s/protocol/openid-connect/certs}")
    private String keycloakJwksUrl;

    public int adminHttpPort()
    {
        return adminHttpPort;
    }

    public String risingwaveDb()
    {
        return risingwaveDb;
    }

    public String kafkaBootstrapServers()
    {
        return kafkaBootstrapServers;
    }

    public String karapaceUrl()
    {
        return karapaceUrl;
    }

    public String apicurioUrl()
    {
        return apicurioUrl;
    }

    public String apicurioGroupId()
    {
        return apicurioGroupId;
    }

    public String keycloakUrl()
    {
        return keycloakUrl;
    }

    public String keycloakRealm()
    {
        return keycloakRealm;
    }

    public String keycloakAudience()
    {
        return keycloakAudience;
    }

    public String keycloakJwksUrl()
    {
        return keycloakJwksUrl;
    }
}
