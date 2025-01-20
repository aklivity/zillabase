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
package io.aklivity.zillabase.cli.config;

import java.util.ArrayList;
import java.util.List;

public final class ZillabaseKeycloakConfig
{
    private static final String DEFAULT_KEYCLOAK_TAG = "26";
    private static final String DEFAULT_KEYCLOAK_URL = "http://keycloak.zillabase.dev:8180";
    private static final String DEFAULT_KEYCLOAK_JWKS_URL =
        "http://keycloak.zillabase.dev:8180/realms/%s/protocol/openid-connect/certs";
    private static final String DEFAULT_KEYCLOAK_AUDIENCE = "account";

    public String tag = DEFAULT_KEYCLOAK_TAG;
    public String url = DEFAULT_KEYCLOAK_URL;
    public String audience = DEFAULT_KEYCLOAK_AUDIENCE;
    public String jwks = DEFAULT_KEYCLOAK_JWKS_URL;
    public String realm;
    public ZillabaseKeycloakClientConfig client;
    public List<ZillabaseKeycloakUserConfig> users;
    public List<String> scopes = new ArrayList<>();
}
