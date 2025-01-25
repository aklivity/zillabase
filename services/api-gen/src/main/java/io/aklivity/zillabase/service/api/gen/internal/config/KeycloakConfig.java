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

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig
{
    @Value("${keycloak.url:http://keycloak.zillabase.dev:8180}")
    private String serverUrl;

    @Value("${keycloak.audience:account}")
    private String audience;

    @Value("${keycloak.jwks.url:http://keycloak.zillabase.dev:8180/realms/%s/protocol/openid-connect/certs}")
    private String jwksUrl;

    @Value("${keycloak.realm:zillabase}")
    private String realm;

    @Value("${keycloak.client-id:admin-cli}")
    private String clientId;

    @Value("${keycloak.client-secret:@null}")
    private String clientSecret;

    @Value("${keycloak.username:admin}")
    private String username;

    @Value("${keycloak.password:admin}")
    private String password;

    public String serverUrl()
    {
        return serverUrl;
    }

    public String realm()
    {
        return realm;
    }

    public String audience()
    {
        return audience;
    }

    public String jwksUrl()
    {
        return jwksUrl;
    }

    @Bean
    public Keycloak keycloak()
    {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .grantType(OAuth2Constants.PASSWORD)
                .clientId(clientId)
                .username(username)
                .password(password)
                .build();
    }
}
